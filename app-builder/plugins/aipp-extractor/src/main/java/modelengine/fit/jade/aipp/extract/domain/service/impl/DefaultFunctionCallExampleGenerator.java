/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service.impl;

import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.FunctionCallExampleGenerator;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 默认的 function calling 示例生成器实现。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class DefaultFunctionCallExampleGenerator implements FunctionCallExampleGenerator {
    
    private static final Logger log = Logger.get(DefaultFunctionCallExampleGenerator.class);
    
    private final ObjectSerializer serializer;
    private final Random random = new Random();
    
    /**
     * 构造函数。
     *
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     */
    public DefaultFunctionCallExampleGenerator(@Fit(alias = "json") ObjectSerializer serializer) {
        this.serializer = notNull(serializer, "The serializer cannot be null.");
    }
    
    @Override
    public List<String> generateExamples(ToolInfo toolInfo) {
        notNull(toolInfo, "The tool info cannot be null.");
        
        List<String> examples = new ArrayList<>();
        
        try {
            // 生成正确示例
            String correctExample = generateCorrectExample(toolInfo);
            if (correctExample != null) {
                examples.add(correctExample);
            }
            
            // 生成错误避免示例
            String errorExample = generateErrorAvoidanceExample(toolInfo);
            if (errorExample != null) {
                examples.add(errorExample);
            }
            
            log.debug("Generated {} examples for tool [{}]", examples.size(), toolInfo.name());
            
        } catch (Exception e) {
            log.warn("Failed to generate examples for tool [{}]: {}", toolInfo.name(), e.getMessage());
        }
        
        return examples;
    }
    
    @Override
    public String generateCorrectExample(ToolInfo toolInfo) {
        notNull(toolInfo, "The tool info cannot be null.");
        
        try {
            Map<String, Object> parameters = toolInfo.parameters();
            Map<String, Object> properties = ObjectUtils.cast(parameters.get("properties"));
            
            if (properties == null || properties.isEmpty()) {
                return null;
            }
            
            StringBuilder example = new StringBuilder();
            example.append("✅ **正确示例**：\n");
            example.append("调用工具 `").append(toolInfo.name()).append("` 时：\n");
            example.append("```json\n");
            
            Map<String, Object> sampleArgs = generateSampleArguments(properties);
            example.append(serializer.serialize(sampleArgs));
            
            example.append("\n```");
            
            return example.toString();
            
        } catch (Exception e) {
            log.warn("Failed to generate correct example for tool [{}]: {}", toolInfo.name(), e.getMessage());
            return null;
        }
    }
    
    @Override
    public String generateErrorAvoidanceExample(ToolInfo toolInfo) {
        notNull(toolInfo, "The tool info cannot be null.");
        
        try {
            StringBuilder example = new StringBuilder();
            example.append("❌ **常见错误避免**：\n");
            
            // 类型错误示例
            example.append("- 避免字符串不加双引号：`{\"name\": hello}` ❌，应该是：`{\"name\": \"hello\"}` ✅\n");
            example.append("- 避免数字用引号：`{\"age\": \"25\"}` ❌，应该是：`{\"age\": 25}` ✅\n");
            example.append("- 避免布尔值用引号：`{\"active\": \"true\"}` ❌，应该是：`{\"active\": true}` ✅\n");
            example.append("- 避免多余的逗号：`{\"name\": \"hello\",}` ❌，应该是：`{\"name\": \"hello\"}` ✅\n");
            example.append("- 避免中文标点：`{\"name\"：\"hello\"，\"age\"：25}` ❌，应该使用英文标点 ✅\n");
            
            return example.toString();
            
        } catch (Exception e) {
            log.warn("Failed to generate error avoidance example for tool [{}]: {}", toolInfo.name(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成示例参数。
     *
     * @param properties 参数属性定义
     * @return 示例参数映射
     */
    private Map<String, Object> generateSampleArguments(Map<String, Object> properties) {
        Map<String, Object> sampleArgs = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String paramName = entry.getKey();
            Map<String, Object> paramDef = ObjectUtils.cast(entry.getValue());
            
            if (paramDef != null) {
                Object sampleValue = generateSampleValue(paramName, paramDef);
                if (sampleValue != null) {
                    sampleArgs.put(paramName, sampleValue);
                }
            }
        }
        
        return sampleArgs;
    }
    
    /**
     * 生成示例值。
     *
     * @param paramName 参数名称
     * @param paramDef 参数定义
     * @return 示例值
     */
    private Object generateSampleValue(String paramName, Map<String, Object> paramDef) {
        String type = ObjectUtils.cast(paramDef.get("type"));
        String description = ObjectUtils.cast(paramDef.get("description"));
        
        if (type == null) {
            return "示例值";
        }
        
        return switch (type.toLowerCase()) {
            case "string" -> generateSampleString(paramName, description);
            case "integer", "number" -> generateSampleNumber(paramName, description);
            case "boolean" -> generateSampleBoolean(paramName);
            case "array" -> generateSampleArray(paramDef);
            case "object" -> generateSampleObject(paramDef);
            default -> "示例值";
        };
    }
    
    /**
     * 生成示例字符串。
     */
    private String generateSampleString(String paramName, String description) {
        // 根据参数名和描述生成合适的示例
        if (paramName.toLowerCase().contains("name")) {
            return "张三";
        } else if (paramName.toLowerCase().contains("text") || paramName.toLowerCase().contains("content")) {
            return "这是示例文本内容";
        } else if (paramName.toLowerCase().contains("desc")) {
            return "这是描述信息";
        } else if (paramName.toLowerCase().contains("id")) {
            return "sample_id_" + random.nextInt(1000);
        } else if (paramName.toLowerCase().contains("url")) {
            return "https://example.com";
        } else if (paramName.toLowerCase().contains("email")) {
            return "example@test.com";
        } else {
            return "示例" + paramName;
        }
    }
    
    /**
     * 生成示例数字。
     */
    private Number generateSampleNumber(String paramName, String description) {
        if (paramName.toLowerCase().contains("age")) {
            return 25;
        } else if (paramName.toLowerCase().contains("count") || paramName.toLowerCase().contains("num")) {
            return 10;
        } else if (paramName.toLowerCase().contains("price") || paramName.toLowerCase().contains("amount")) {
            return 99.99;
        } else {
            return 123;
        }
    }
    
    /**
     * 生成示例布尔值。
     */
    private Boolean generateSampleBoolean(String paramName) {
        return paramName.toLowerCase().contains("enable") || 
               paramName.toLowerCase().contains("active") || 
               paramName.toLowerCase().contains("valid");
    }
    
    /**
     * 生成示例数组。
     */
    private List<Object> generateSampleArray(Map<String, Object> paramDef) {
        List<Object> array = new ArrayList<>();
        Map<String, Object> items = ObjectUtils.cast(paramDef.get("items"));
        
        if (items != null) {
            // 生成1-2个示例元素
            for (int i = 0; i < Math.min(2, random.nextInt(2) + 1); i++) {
                Object sampleItem = generateSampleValue("item", items);
                if (sampleItem != null) {
                    array.add(sampleItem);
                }
            }
        } else {
            array.add("示例项1");
            array.add("示例项2");
        }
        
        return array;
    }
    
    /**
     * 生成示例对象。
     */
    private Map<String, Object> generateSampleObject(Map<String, Object> paramDef) {
        Map<String, Object> obj = new HashMap<>();
        Map<String, Object> properties = ObjectUtils.cast(paramDef.get("properties"));
        
        if (properties != null) {
            // 只生成前3个属性作为示例
            int count = 0;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (count >= 3) break;
                
                String propName = entry.getKey();
                Map<String, Object> propDef = ObjectUtils.cast(entry.getValue());
                
                if (propDef != null) {
                    Object sampleValue = generateSampleValue(propName, propDef);
                    if (sampleValue != null) {
                        obj.put(propName, sampleValue);
                        count++;
                    }
                }
            }
        }
        
        if (obj.isEmpty()) {
            obj.put("示例属性", "示例值");
        }
        
        return obj;
    }
} 