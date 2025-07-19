/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.impl.DefaultSmartArgumentsParser;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.util.MapBuilder;
import modelengine.fitframework.util.TypeUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能参数解析器测试类。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@DisplayName("智能参数解析器测试")
class SmartArgumentsParserTest {
    
    private DefaultSmartArgumentsParser smartParser;
    private ObjectSerializer mockSerializer;
    private ParameterValidator mockValidator;
    private ParameterFixer mockFixer;
    
    @BeforeEach
    void setUp() {
        mockSerializer = mock(ObjectSerializer.class);
        mockValidator = mock(ParameterValidator.class);
        mockFixer = mock(ParameterFixer.class);
        
        smartParser = new DefaultSmartArgumentsParser(mockSerializer, mockValidator, mockFixer);
    }
    
    @Test
    @DisplayName("测试直接解析成功的情况")
    void shouldParseDirectlyWhenValidJson() throws Exception {
        // Given
        String validJson = "{\"name\":\"张三\",\"age\":25}";
        Map<String, Object> expectedResult = MapBuilder.<String, Object>get()
            .put("name", "张三")
            .put("age", 25)
            .build();
        
        ToolInfo toolInfo = createTestToolInfo();
        
        when(mockSerializer.deserialize(anyString(), any(Type.class))).thenReturn(expectedResult);
        when(mockValidator.isValid(any(), any())).thenReturn(true);
        
        // When
        Map<String, Object> result = smartParser.parseArguments(validJson, toolInfo);
        
        // Then
        assertNotNull(result);
        assertEquals("张三", result.get("name"));
        assertEquals(25, result.get("age"));
    }
    
    @Test
    @DisplayName("测试清理后解析成功的情况")
    void shouldParseAfterCleaningWhenDirtyJson() throws Exception {
        // Given
        String dirtyJson = "{\"name\"：\"张三\"，\"age\"：25}"; // 中文标点
        String cleanedJson = "{\"name\":\"张三\",\"age\":25}";
        Map<String, Object> expectedResult = MapBuilder.<String, Object>get()
            .put("name", "张三")
            .put("age", 25)
            .build();
        
        ToolInfo toolInfo = createTestToolInfo();
        
        // 第一次直接解析失败
        when(mockSerializer.deserialize(dirtyJson, TypeUtils.parameterized(Map.class, String.class, Object.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));
        
        // 清理后解析成功
        when(mockFixer.cleanJson(dirtyJson)).thenReturn(cleanedJson);
        when(mockSerializer.deserialize(cleanedJson, TypeUtils.parameterized(Map.class, String.class, Object.class)))
            .thenReturn(expectedResult);
        when(mockValidator.isValid(expectedResult, toolInfo)).thenReturn(true);
        
        // When
        Map<String, Object> result = smartParser.parseArguments(dirtyJson, toolInfo);
        
        // Then
        assertNotNull(result);
        assertEquals("张三", result.get("name"));
    }
    
    @Test
    @DisplayName("测试智能修复成功的情况")
    void shouldParseWithIntelligentFixWhenBrokenJson() {
        // Given
        String brokenJson = "name: 张三, age: 25"; // 非标准JSON格式
        Map<String, Object> fixedResult = MapBuilder.<String, Object>get()
            .put("name", "张三")
            .put("age", 25L)
            .build();
        
        ToolInfo toolInfo = createTestToolInfo();
        
        // 前面的尝试都失败
        when(mockSerializer.deserialize(anyString(), any(Type.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));
        when(mockFixer.cleanJson(anyString())).thenReturn(brokenJson);
        when(mockValidator.isValid(any(), any())).thenReturn(false).thenReturn(true);
        
        // 智能修复成功
        when(mockFixer.intelligentFix(brokenJson, toolInfo)).thenReturn(fixedResult);
        
        // When
        Map<String, Object> result = smartParser.parseArguments(brokenJson, toolInfo);
        
        // Then
        assertNotNull(result);
        assertEquals("张三", result.get("name"));
        assertEquals(25L, result.get("age"));
    }
    
    @Test
    @DisplayName("测试所有解析方法都失败的情况")
    void shouldThrowExceptionWhenAllParsingFails() {
        // Given
        String invalidJson = "completely broken json";
        ToolInfo toolInfo = createTestToolInfo();
        
        // 所有解析方法都失败
        when(mockSerializer.deserialize(anyString(), any(Type.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));
        when(mockFixer.cleanJson(anyString())).thenReturn(invalidJson);
        when(mockValidator.isValid(any(), any())).thenReturn(false);
        when(mockFixer.intelligentFix(anyString(), any())).thenReturn(new HashMap<>());
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            smartParser.parseArguments(invalidJson, toolInfo);
        });
    }
    
    @Test
    @DisplayName("测试部分修复成功的情况")
    void shouldReturnPartialResultWhenPartialFixSucceeds() {
        // Given
        String invalidJson = "broken json";
        Map<String, Object> partialResult = MapBuilder.<String, Object>get()
            .put("name", "默认名称")  // 缺少age字段
            .build();
        
        Map<String, Object> enhancedResult = MapBuilder.<String, Object>get()
            .put("name", "默认名称")
            .put("age", 0L)  // 补充的默认值
            .build();
        
        ToolInfo toolInfo = createTestToolInfo();
        
        // 前面的尝试都失败，但智能修复返回部分结果
        when(mockSerializer.deserialize(anyString(), any(Type.class)))
            .thenThrow(new RuntimeException("Invalid JSON"));
        when(mockFixer.cleanJson(anyString())).thenReturn(invalidJson);
        when(mockFixer.intelligentFix(invalidJson, toolInfo)).thenReturn(partialResult);
        when(mockValidator.isValid(partialResult, toolInfo)).thenReturn(false);
        when(mockValidator.checkRequiredFields(enhancedResult, toolInfo)).thenReturn(true);
        
        // When
        Map<String, Object> result = smartParser.parseArguments(invalidJson, toolInfo);
        
        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("name"));
    }
    
    private ToolInfo createTestToolInfo() {
        return ToolInfo.custom()
            .name("test_tool")
            .description("测试工具")
            .parameters(MapBuilder.<String, Object>get()
                .put("type", "object")
                .put("properties", MapBuilder.<String, Object>get()
                    .put("name", MapBuilder.<String, Object>get()
                        .put("type", "string")
                        .put("description", "姓名")
                        .build())
                    .put("age", MapBuilder.<String, Object>get()
                        .put("type", "integer")
                        .put("description", "年龄")
                        .build())
                    .build())
                .put("required", Arrays.asList("name", "age"))
                .build())
            .build();
    }
} 