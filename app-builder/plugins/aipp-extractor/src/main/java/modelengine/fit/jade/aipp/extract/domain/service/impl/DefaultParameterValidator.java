/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service.impl;

import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.ParameterValidator;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fitframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 默认参数验证器实现。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class DefaultParameterValidator implements ParameterValidator {
    
    private static final Logger log = Logger.get(DefaultParameterValidator.class);
    
    @Override
    public boolean isValid(Map<String, Object> arguments, ToolInfo toolInfo) {
        notNull(arguments, "The arguments cannot be null.");
        notNull(toolInfo, "The tool info cannot be null.");
        
        try {
            // 检查必填参数
            if (!checkRequiredFields(arguments, toolInfo)) {
                log.debug("Required fields validation failed for tool [{}]", toolInfo.name());
                return false;
            }
            
            // 检查参数类型
            if (!checkParameterTypes(arguments, toolInfo)) {
                log.debug("Parameter types validation failed for tool [{}]", toolInfo.name());
                return false;
            }
            
            log.debug("Parameter validation passed for tool [{}]", toolInfo.name());
            return true;
            
        } catch (Exception e) {
            log.warn("Parameter validation error for tool [{}]: {}", toolInfo.name(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean checkRequiredFields(Map<String, Object> arguments, ToolInfo toolInfo) {
        try {
            Map<String, Object> parameters = toolInfo.parameters();
            if (parameters == null) {
                return true;
            }
            
            List<String> required = ObjectUtils.cast(parameters.get("required"));
            if (required == null || required.isEmpty()) {
                return true;
            }
            
            for (String field : required) {
                if (!arguments.containsKey(field)) {
                    log.debug("Missing required field [{}] in tool [{}]", field, toolInfo.name());
                    return false;
                }
                
                Object value = arguments.get(field);
                if (value == null) {
                    log.debug("Required field [{}] is null in tool [{}]", field, toolInfo.name());
                    return false;
                }
                
                // 检查字符串是否为空
                if (value instanceof String && StringUtils.isBlank((String) value)) {
                    log.debug("Required field [{}] is blank in tool [{}]", field, toolInfo.name());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Required fields check error for tool [{}]: {}", toolInfo.name(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean checkParameterTypes(Map<String, Object> arguments, ToolInfo toolInfo) {
        try {
            Map<String, Object> parameters = toolInfo.parameters();
            if (parameters == null) {
                return true;
            }
            
            Map<String, Object> properties = ObjectUtils.cast(parameters.get("properties"));
            if (properties == null || properties.isEmpty()) {
                return true;
            }
            
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                if (!properties.containsKey(paramName)) {
                    log.debug("Unknown parameter [{}] in tool [{}]", paramName, toolInfo.name());
                    continue; // 允许额外参数，只验证已定义的参数
                }
                
                Map<String, Object> paramDef = ObjectUtils.cast(properties.get(paramName));
                if (paramDef == null) {
                    continue;
                }
                
                if (!isValidType(paramValue, paramDef)) {
                    log.debug("Invalid type for parameter [{}] in tool [{}], expected [{}], got [{}]", 
                        paramName, toolInfo.name(), paramDef.get("type"), 
                        paramValue != null ? paramValue.getClass().getSimpleName() : "null");
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Parameter types check error for tool [{}]: {}", toolInfo.name(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查值是否符合指定类型。
     *
     * @param value 参数值
     * @param paramDef 参数定义
     * @return 当类型匹配时返回true
     */
    private boolean isValidType(Object value, Map<String, Object> paramDef) {
        if (value == null) {
            return true; // null值在必填检查中处理
        }
        
        String expectedType = ObjectUtils.cast(paramDef.get("type"));
        if (expectedType == null) {
            return true; // 没有类型定义时认为有效
        }
        
        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long || 
                            (value instanceof String && isInteger((String) value));
            case "number" -> value instanceof Number || 
                           (value instanceof String && isNumber((String) value));
            case "boolean" -> value instanceof Boolean || 
                            (value instanceof String && isBoolean((String) value));
            case "array" -> value instanceof List || value instanceof Object[];
            case "object" -> value instanceof Map;
            default -> true; // 未知类型认为有效
        };
    }
    
    /**
     * 检查字符串是否为整数。
     */
    private boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为数字。
     */
    private boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为布尔值。
     */
    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }
} 