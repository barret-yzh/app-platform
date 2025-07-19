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
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fitframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认参数修复器实现。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class DefaultParameterFixer implements ParameterFixer {
    
    private static final Logger log = Logger.get(DefaultParameterFixer.class);
    
    /**
     * 键值对提取正则表达式。
     */
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
        "\"?([^\"\\s:：]+)\"?\\s*[:：]\\s*\"?([^\"\\n,}]+)\"?");
    
    /**
     * JSON对象提取正则表达式。
     */
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
        "\\{([^}]*)\\}");
    
    @Override
    public String cleanJson(String json) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        
        return json
            // 替换中文标点符号
            .replaceAll("，", ",")
            .replaceAll(""", "\"")
            .replaceAll(""", "\"")
            .replaceAll("：", ":")
            .replaceAll("；", ";")
            .replaceAll("（", "(")
            .replaceAll("）", ")")
            .replaceAll("【", "[")
            .replaceAll("】", "]")
            .replaceAll("｛", "{")
            .replaceAll("｝", "}")
            // 清理多余的空白字符
            .replaceAll("\\s+", " ")
            // 清理尾随逗号
            .replaceAll(",\\s*([}\\]])", "$1")
            // 清理多余的逗号
            .replaceAll(",{2,}", ",")
            .trim();
    }
    
    @Override
    public Map<String, Object> fixParameters(Map<String, Object> arguments, ToolInfo toolInfo) {
        notNull(arguments, "The arguments cannot be null.");
        notNull(toolInfo, "The tool info cannot be null.");
        
        Map<String, Object> fixedArgs = new HashMap<>();
        Map<String, Object> parameters = toolInfo.parameters();
        Map<String, Object> properties = ObjectUtils.cast(parameters.get("properties"));
        
        if (properties == null) {
            return arguments;
        }
        
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (properties.containsKey(key)) {
                Map<String, Object> paramDef = ObjectUtils.cast(properties.get(key));
                Object fixedValue = fixValue(value, paramDef);
                fixedArgs.put(key, fixedValue);
            } else {
                // 尝试模糊匹配参数名
                String matchedKey = findBestMatchKey(key, properties.keySet());
                if (matchedKey != null) {
                    Map<String, Object> paramDef = ObjectUtils.cast(properties.get(matchedKey));
                    Object fixedValue = fixValue(value, paramDef);
                    fixedArgs.put(matchedKey, fixedValue);
                    log.debug("Mapped parameter [{}] to [{}] for tool [{}]", key, matchedKey, toolInfo.name());
                } else {
                    fixedArgs.put(key, value);
                }
            }
        }
        
        return fixedArgs;
    }
    
    @Override
    public Map<String, Object> intelligentFix(String originalJson, ToolInfo toolInfo) {
        notBlank(originalJson, "The original JSON cannot be blank.");
        notNull(toolInfo, "The tool info cannot be null.");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> parameters = toolInfo.parameters();
        Map<String, Object> properties = ObjectUtils.cast(parameters.get("properties"));
        
        if (properties == null) {
            return result;
        }
        
        try {
            // 首先尝试清理后的JSON
            String cleanedJson = cleanJson(originalJson);
            log.debug("Cleaned JSON: {}", cleanedJson);
            
            // 尝试提取JSON对象
            Matcher jsonMatcher = JSON_OBJECT_PATTERN.matcher(cleanedJson);
            String jsonContent = jsonMatcher.find() ? jsonMatcher.group(1) : cleanedJson;
            
            // 使用正则表达式提取键值对
            Matcher matcher = KEY_VALUE_PATTERN.matcher(jsonContent);
            while (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                
                // 查找最佳匹配的参数名
                String bestMatchKey = findBestMatchKey(key, properties.keySet());
                if (bestMatchKey != null) {
                    Map<String, Object> paramDef = ObjectUtils.cast(properties.get(bestMatchKey));
                    Object convertedValue = convertValue(value, paramDef);
                    result.put(bestMatchKey, convertedValue);
                    log.debug("Extracted parameter [{}] -> [{}] = [{}]", key, bestMatchKey, convertedValue);
                } else {
                    // 如果没有匹配的参数定义，尝试直接使用
                    Object convertedValue = convertValue(value, null);
                    result.put(key, convertedValue);
                    log.debug("Extracted unknown parameter [{}] = [{}]", key, convertedValue);
                }
            }
            
            log.debug("Intelligent fix extracted {} parameters for tool [{}]", result.size(), toolInfo.name());
            
        } catch (Exception e) {
            log.warn("Intelligent fix failed for tool [{}]: {}", toolInfo.name(), e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public Object convertValue(String value, Object paramSchema) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        
        // 去除首尾引号
        String cleanValue = value.trim();
        if (cleanValue.startsWith("\"") && cleanValue.endsWith("\"")) {
            cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
        }
        
        if (paramSchema == null) {
            return autoDetectType(cleanValue);
        }
        
        Map<String, Object> paramDef = ObjectUtils.cast(paramSchema);
        if (paramDef == null) {
            return autoDetectType(cleanValue);
        }
        
        String type = ObjectUtils.cast(paramDef.get("type"));
        if (type == null) {
            return autoDetectType(cleanValue);
        }
        
        try {
            return switch (type.toLowerCase()) {
                case "string" -> cleanValue;
                case "integer" -> parseInteger(cleanValue);
                case "number" -> parseNumber(cleanValue);
                case "boolean" -> parseBoolean(cleanValue);
                case "array" -> parseArray(cleanValue);
                case "object" -> parseObject(cleanValue);
                default -> cleanValue;
            };
        } catch (Exception e) {
            log.debug("Failed to convert value [{}] to type [{}], using string: {}", cleanValue, type, e.getMessage());
            return cleanValue;
        }
    }
    
    /**
     * 修复单个值。
     */
    private Object fixValue(Object value, Map<String, Object> paramDef) {
        if (value == null || paramDef == null) {
            return value;
        }
        
        String type = ObjectUtils.cast(paramDef.get("type"));
        if (type == null) {
            return value;
        }
        
        // 如果值已经是正确类型，直接返回
        if (isCorrectType(value, type)) {
            return value;
        }
        
        // 尝试类型转换
        if (value instanceof String) {
            return convertValue((String) value, paramDef);
        }
        
        return value;
    }
    
    /**
     * 检查值是否为正确类型。
     */
    private boolean isCorrectType(Object value, String type) {
        return switch (type.toLowerCase()) {
            case "string" -> value instanceof String;
            case "integer" -> value instanceof Integer || value instanceof Long;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List || value instanceof Object[];
            case "object" -> value instanceof Map;
            default -> true;
        };
    }
    
    /**
     * 查找最佳匹配的参数名。
     */
    private String findBestMatchKey(String key, Iterable<String> candidateKeys) {
        String cleanKey = key.toLowerCase().trim();
        
        // 完全匹配
        for (String candidate : candidateKeys) {
            if (candidate.toLowerCase().equals(cleanKey)) {
                return candidate;
            }
        }
        
        // 部分匹配
        for (String candidate : candidateKeys) {
            String cleanCandidate = candidate.toLowerCase();
            if (cleanCandidate.contains(cleanKey) || cleanKey.contains(cleanCandidate)) {
                return candidate;
            }
        }
        
        // 相似度匹配（简单的编辑距离）
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (String candidate : candidateKeys) {
            int distance = editDistance(cleanKey, candidate.toLowerCase());
            if (distance < minDistance && distance <= Math.max(cleanKey.length(), candidate.length()) / 2) {
                minDistance = distance;
                bestMatch = candidate;
            }
        }
        
        return bestMatch;
    }
    
    /**
     * 计算编辑距离。
     */
    private int editDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * 自动检测类型。
     */
    private Object autoDetectType(String value) {
        // 尝试解析为布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // 尝试解析为整数
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }
        
        // 尝试解析为浮点数
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }
        
        // 默认为字符串
        return value;
    }
    
    /**
     * 解析整数。
     */
    private Long parseInteger(String value) {
        return Long.parseLong(value);
    }
    
    /**
     * 解析数字。
     */
    private Double parseNumber(String value) {
        return Double.parseDouble(value);
    }
    
    /**
     * 解析布尔值。
     */
    private Boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 解析数组。
     */
    private List<Object> parseArray(String value) {
        List<Object> result = new ArrayList<>();
        
        // 简单的数组解析，支持逗号分隔
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        
        String[] items = value.split(",");
        for (String item : items) {
            String trimmedItem = item.trim();
            if (trimmedItem.startsWith("\"") && trimmedItem.endsWith("\"")) {
                trimmedItem = trimmedItem.substring(1, trimmedItem.length() - 1);
            }
            result.add(autoDetectType(trimmedItem));
        }
        
        return result;
    }
    
    /**
     * 解析对象。
     */
    private Map<String, Object> parseObject(String value) {
        Map<String, Object> result = new HashMap<>();
        
        // 简单的对象解析
        if (value.startsWith("{") && value.endsWith("}")) {
            value = value.substring(1, value.length() - 1);
        }
        
        Matcher matcher = KEY_VALUE_PATTERN.matcher(value);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String val = matcher.group(2).trim();
            
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            
            result.put(key, autoDetectType(val));
        }
        
        return result;
    }
} 