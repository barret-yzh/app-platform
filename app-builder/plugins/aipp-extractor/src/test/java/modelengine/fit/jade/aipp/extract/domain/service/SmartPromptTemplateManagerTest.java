/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.impl.SmartPromptTemplateManager;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.util.MapBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

/**
 * 智能提示词模板管理器测试类。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@DisplayName("智能提示词模板管理器测试")
class SmartPromptTemplateManagerTest {
    
    private SmartPromptTemplateManager promptTemplateManager;
    private FunctionCallExampleGenerator mockExampleGenerator;
    private ObjectSerializer mockSerializer;
    
    @BeforeEach
    void setUp() {
        mockExampleGenerator = mock(FunctionCallExampleGenerator.class);
        mockSerializer = mock(ObjectSerializer.class);
        
        // 模拟示例生成器行为
        when(mockExampleGenerator.generateExamples(any(ToolInfo.class)))
            .thenReturn(Collections.singletonList("示例调用"));
        
        // 模拟序列化器行为
        when(mockSerializer.serialize(any())).thenReturn("{\"type\":\"object\"}");
        
        promptTemplateManager = new SmartPromptTemplateManager(mockExampleGenerator, mockSerializer);
    }
    
    @Test
    @DisplayName("测试基础提示词模板获取")
    void shouldReturnBaseTemplate() {
        // When
        String baseTemplate = promptTemplateManager.getBaseTemplate();
        
        // Then
        assertNotNull(baseTemplate);
        assertTrue(baseTemplate.contains("你是一个精确的工具调用专家"));
        assertTrue(baseTemplate.contains("核心规则"));
    }
    
    @Test
    @DisplayName("测试Qwen模型特定提示词")
    void shouldReturnQwenSpecificPrompt() {
        // When
        String qwenPrompt = promptTemplateManager.getModelSpecificPrompt("Qwen2-72B-Instruct");
        
        // Then
        assertNotNull(qwenPrompt);
        assertTrue(qwenPrompt.toLowerCase().contains("qwen"));
        assertTrue(qwenPrompt.contains("JSON格式严格正确"));
    }
    
    @Test
    @DisplayName("测试ChatGLM模型特定提示词")
    void shouldReturnChatGLMSpecificPrompt() {
        // When
        String chatglmPrompt = promptTemplateManager.getModelSpecificPrompt("chatglm3-6b");
        
        // Then
        assertNotNull(chatglmPrompt);
        assertTrue(chatglmPrompt.toLowerCase().contains("chatglm"));
        assertTrue(chatglmPrompt.contains("数组格式"));
    }
    
    @Test
    @DisplayName("测试生成完整的function calling提示词")
    void shouldGenerateCompletePrompt() {
        // Given
        ToolInfo toolInfo = ToolInfo.custom()
            .name("test_tool")
            .description("测试工具")
            .parameters(MapBuilder.<String, Object>get()
                .put("type", "object")
                .put("properties", MapBuilder.<String, Object>get()
                    .put("name", MapBuilder.<String, Object>get()
                        .put("type", "string")
                        .put("description", "名称")
                        .build())
                    .build())
                .build())
            .build();
        
        Map<String, Object> variables = MapBuilder.<String, Object>get()
            .put("text", "测试输入文本")
            .put("desc", "提取名称信息")
            .put("histories", "历史对话记录")
            .build();
        
        // When
        String prompt = promptTemplateManager.getFunctionCallingPrompt("Qwen2-72B", toolInfo, variables);
        
        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("test_tool"));
        assertTrue(prompt.contains("测试工具"));
        assertTrue(prompt.contains("测试输入文本"));
        assertTrue(prompt.contains("提取名称信息"));
        assertTrue(prompt.contains("历史对话记录"));
        assertTrue(prompt.contains("Qwen模型特别提醒"));
    }
    
    @Test
    @DisplayName("测试未知模型的提示词生成")
    void shouldGeneratePromptForUnknownModel() {
        // Given
        ToolInfo toolInfo = ToolInfo.custom()
            .name("test_tool")
            .description("测试工具")
            .parameters(MapBuilder.<String, Object>get()
                .put("type", "object")
                .build())
            .build();
        
        Map<String, Object> variables = MapBuilder.<String, Object>get()
            .put("text", "测试文本")
            .build();
        
        // When
        String prompt = promptTemplateManager.getFunctionCallingPrompt("unknown_model", toolInfo, variables);
        
        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("test_tool"));
        // 未知模型不应该包含特定的模型提示
        assertTrue(!prompt.contains("特别注意") || prompt.contains("## 调用示例"));
    }
} 