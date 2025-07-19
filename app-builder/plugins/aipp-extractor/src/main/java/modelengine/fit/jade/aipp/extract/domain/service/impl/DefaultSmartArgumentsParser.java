/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service.impl;

import static modelengine.fitframework.inspection.Validation.notBlank;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.ParameterFixer;
import modelengine.fit.jade.aipp.extract.domain.service.ParameterValidator;
import modelengine.fit.jade.aipp.extract.domain.service.SmartArgumentsParser;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.util.TypeUtils;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * 默认智能参数解析器实现，支持多次尝试和自动修复。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class DefaultSmartArgumentsParser implements SmartArgumentsParser {
    
    private static final Logger log = Logger.get(DefaultSmartArgumentsParser.class);
    
    private final ObjectSerializer serializer;
    private final ParameterValidator validator;
    private final ParameterFixer parameterFixer;
    
    /**
     * 构造函数。
     *
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @param validator 表示参数验证器的 {@link ParameterValidator}。
     * @param parameterFixer 表示参数修复器的 {@link ParameterFixer}。
     */
    public DefaultSmartArgumentsParser(@Fit(alias = "json") ObjectSerializer serializer,
                                     ParameterValidator validator,
                                     ParameterFixer parameterFixer) {
        this.serializer = notNull(serializer, "The serializer cannot be null.");
        this.validator = notNull(validator, "The validator cannot be null.");
        this.parameterFixer = notNull(parameterFixer, "The parameter fixer cannot be null.");
    }
    
    @Override
    public Map<String, Object> parseArguments(String argumentsJson, ToolInfo toolInfo) {
        notBlank(argumentsJson, "The arguments JSON cannot be blank.");
        notNull(toolInfo, "The tool info cannot be null.");
        
        log.debug("Starting smart parsing for tool [{}] with arguments: {}", toolInfo.name(), argumentsJson);
        
        // 第一次尝试：直接解析
        try {
            Map<String, Object> args = directParse(argumentsJson);
            if (validator.isValid(args, toolInfo)) {
                log.debug("Direct parsing successful for tool [{}]", toolInfo.name());
                return args;
            } else {
                log.debug("Direct parsing result validation failed for tool [{}]", toolInfo.name());
            }
        } catch (Exception e) {
            log.debug("Direct parsing failed for tool [{}]: {}", toolInfo.name(), e.getMessage());
        }
        
        // 第二次尝试：清理和修复
        try {
            String cleanedJson = parameterFixer.cleanJson(argumentsJson);
            Map<String, Object> args = directParse(cleanedJson);
            
            if (validator.isValid(args, toolInfo)) {
                log.debug("Cleaned parsing successful for tool [{}]", toolInfo.name());
                return args;
            }
            
            // 尝试修复参数
            Map<String, Object> fixedArgs = parameterFixer.fixParameters(args, toolInfo);
            if (validator.isValid(fixedArgs, toolInfo)) {
                log.debug("Parameter fixing successful for tool [{}]", toolInfo.name());
                return fixedArgs;
            } else {
                log.debug("Parameter fixing validation failed for tool [{}]", toolInfo.name());
            }
            
        } catch (Exception e) {
            log.debug("Cleaned parsing failed for tool [{}]: {}", toolInfo.name(), e.getMessage());
        }
        
        // 第三次尝试：模糊匹配和智能修复
        try {
            Map<String, Object> intelligentArgs = parameterFixer.intelligentFix(argumentsJson, toolInfo);
            
            if (!intelligentArgs.isEmpty()) {
                if (validator.isValid(intelligentArgs, toolInfo)) {
                    log.debug("Intelligent fix successful for tool [{}]", toolInfo.name());
                    return intelligentArgs;
                } else {
                    log.debug("Intelligent fix validation failed for tool [{}], attempting partial fix", toolInfo.name());
                    
                    // 尝试部分修复，至少保证必填字段
                    Map<String, Object> partialArgs = ensureRequiredFields(intelligentArgs, toolInfo);
                    if (validator.checkRequiredFields(partialArgs, toolInfo)) {
                        log.warn("Using partially fixed arguments for tool [{}]", toolInfo.name());
                        return partialArgs;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Intelligent fix failed for tool [{}]: {}", toolInfo.name(), e.getMessage());
        }
        
        // 所有尝试都失败
        log.error("All parsing attempts failed for tool [{}] with arguments: {}", toolInfo.name(), argumentsJson);
        throw new IllegalStateException(
            String.format("Failed to parse arguments for tool [%s] after all attempts", toolInfo.name()));
    }
    
    @Override
    public Map<String, Object> directParse(String argumentsJson) throws Exception {
        Type mapType = TypeUtils.parameterized(Map.class, new Type[]{String.class, Object.class});
        return serializer.deserialize(argumentsJson, mapType);
    }
    
    /**
     * 确保必填字段存在。
     *
     * @param arguments 当前参数
     * @param toolInfo 工具信息
     * @return 补全后的参数
     */
    private Map<String, Object> ensureRequiredFields(Map<String, Object> arguments, ToolInfo toolInfo) {
        Map<String, Object> parameters = toolInfo.parameters();
        if (parameters == null) {
            return arguments;
        }
        
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) parameters.get("required");
        if (required == null || required.isEmpty()) {
            return arguments;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        if (properties == null) {
            return arguments;
        }
        
        // 为缺失的必填字段添加默认值
        for (String field : required) {
            if (!arguments.containsKey(field) || arguments.get(field) == null) {
                Object defaultValue = generateDefaultValue(field, properties.get(field));
                if (defaultValue != null) {
                    arguments.put(field, defaultValue);
                    log.debug("Added default value for required field [{}] in tool [{}]", field, toolInfo.name());
                }
            }
        }
        
        return arguments;
    }
    
    /**
     * 生成默认值。
     *
     * @param fieldName 字段名
     * @param fieldDef 字段定义
     * @return 默认值
     */
    private Object generateDefaultValue(String fieldName, Object fieldDef) {
        if (fieldDef == null) {
            return "默认值";
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> def = (Map<String, Object>) fieldDef;
        String type = (String) def.get("type");
        
        if (type == null) {
            return "默认值";
        }
        
        return switch (type.toLowerCase()) {
            case "string" -> generateDefaultString(fieldName);
            case "integer" -> 0L;
            case "number" -> 0.0;
            case "boolean" -> false;
            case "array" -> new java.util.ArrayList<>();
            case "object" -> new java.util.HashMap<>();
            default -> "默认值";
        };
    }
    
    /**
     * 生成默认字符串值。
     */
    private String generateDefaultString(String fieldName) {
        String lowerName = fieldName.toLowerCase();
        
        if (lowerName.contains("text") || lowerName.contains("content")) {
            return "默认文本内容";
        } else if (lowerName.contains("desc") || lowerName.contains("description")) {
            return "默认描述";
        } else if (lowerName.contains("name")) {
            return "默认名称";
        } else if (lowerName.contains("id")) {
            return "default_id";
        } else if (lowerName.contains("url")) {
            return "https://example.com";
        } else {
            return "默认值";
        }
    }
} 