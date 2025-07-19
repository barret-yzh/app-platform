/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.entity.support;

import static modelengine.fit.jade.aipp.extract.code.ContentExtractRetCode.MODEL_RESPONSE_ERROR;
import static modelengine.fit.jade.aipp.extract.code.ContentExtractRetCode.TOOLCALL_SIZE_ERROR;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.chat.ChatMessage;
import modelengine.fel.core.chat.ChatModel;
import modelengine.fel.core.chat.ChatOption;
import modelengine.fel.core.chat.support.ChatMessages;
import modelengine.fel.core.chat.support.HumanMessage;
import modelengine.fel.core.tool.ToolCall;
import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.code.ModelEngineException;
import modelengine.fit.jade.aipp.extract.domain.entity.ContentExtractor;
import modelengine.fit.jade.aipp.extract.domain.service.PromptTemplateManager;
import modelengine.fit.jade.aipp.extract.domain.service.SmartArgumentsParser;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.serialization.SerializationException;
import modelengine.fitframework.util.MapBuilder;
import modelengine.fitframework.util.ObjectUtils;
import modelengine.fitframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版 function calling 内容提取器，支持智能提示词生成和参数解析。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class EnhancedFunctionCallContentExtractor implements ContentExtractor {
    
    private static final Logger log = Logger.get(EnhancedFunctionCallContentExtractor.class);
    private static final int MODEL_MESSAGE_AMOUNT = 1;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final ChatModel modelService;
    private final PromptTemplateManager promptTemplateManager;
    private final SmartArgumentsParser argumentsParser;
    private final ObjectSerializer serializer;
    
    /**
     * 构造函数。
     *
     * @param modelService 表示模型服务的 {@link ChatModel}。
     * @param promptTemplateManager 表示提示词模板管理器的 {@link PromptTemplateManager}。
     * @param argumentsParser 表示智能参数解析器的 {@link SmartArgumentsParser}。
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     */
    public EnhancedFunctionCallContentExtractor(ChatModel modelService,
                                              PromptTemplateManager promptTemplateManager,
                                              SmartArgumentsParser argumentsParser,
                                              @Fit(alias = "json") ObjectSerializer serializer) {
        this.modelService = notNull(modelService, "The model service cannot be null.");
        this.promptTemplateManager = notNull(promptTemplateManager, "The prompt template manager cannot be null.");
        this.argumentsParser = notNull(argumentsParser, "The arguments parser cannot be null.");
        this.serializer = notNull(serializer, "The serializer cannot be null.");
    }
    
    @Override
    public Object run(Map<String, String> variables, String outputSchema, ChatOption chatOption) {
        notNull(variables, "The extracting variables cannot be null");
        notNull(outputSchema, "The output schema cannot be null");
        notNull(chatOption, "The chat option cannot be null");
        
        String modelName = ObjectUtils.nullIf(chatOption.model(), "unknown");
        log.info("Starting enhanced function call extraction with model [{}]", modelName);
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                Object result = attemptFunctionCall(variables, outputSchema, chatOption, modelName, attempt);
                log.info("Function call extraction successful for model [{}] on attempt {}", modelName, attempt);
                return result;
                
            } catch (ModelEngineException e) {
                log.warn("Function call attempt {} failed for model [{}]: {}", attempt, modelName, e.getMessage());
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw e;
                }
                
                // 在重试前稍作调整
                adjustForRetry(chatOption, attempt);
                
            } catch (Exception e) {
                log.error("Unexpected error in function call attempt {} for model [{}]: {}", 
                    attempt, modelName, e.getMessage());
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    throw new ModelEngineException(MODEL_RESPONSE_ERROR);
                }
            }
        }
        
        throw new ModelEngineException(MODEL_RESPONSE_ERROR);
    }
    
    /**
     * 尝试执行 function calling。
     *
     * @param variables 输入变量
     * @param outputSchema 输出 Schema
     * @param chatOption 聊天选项
     * @param modelName 模型名称
     * @param attempt 尝试次数
     * @return 解析结果
     */
    private Object attemptFunctionCall(Map<String, String> variables, String outputSchema, 
                                     ChatOption chatOption, String modelName, int attempt) {
        
        // 1. 构建增强的工具信息
        ToolInfo tool = buildEnhancedToolInfo(outputSchema, modelName, attempt);
        
        // 2. 生成模型特定的优化提示词
        Map<String, Object> templateVariables = buildTemplateVariables(variables);
        String optimizedPrompt = promptTemplateManager.getFunctionCallingPrompt(modelName, tool, templateVariables);
        
        log.debug("Generated optimized prompt for model [{}], attempt {}: {}", modelName, attempt, 
            optimizedPrompt.length() > 500 ? optimizedPrompt.substring(0, 500) + "..." : optimizedPrompt);
        
        // 3. 调用模型
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.add(new HumanMessage(optimizedPrompt));
        
        ChatOption enhancedOption = enhanceChatOption(chatOption, tool, attempt);
        List<ChatMessage> answer = this.modelService.generate(chatMessages, enhancedOption).blockAll();
        
        if (answer.isEmpty()) {
            log.error("Model returned empty response for attempt {}", attempt);
            throw new ModelEngineException(MODEL_RESPONSE_ERROR);
        }
        
        // 4. 解析和验证结果
        ChatMessage message = answer.get(0);
        List<ToolCall> toolCalls = message.toolCalls();
        
        if (toolCalls.size() != MODEL_MESSAGE_AMOUNT) {
            log.error("Invalid tool calls count: expected {}, got {} for attempt {}", 
                MODEL_MESSAGE_AMOUNT, toolCalls.size(), attempt);
            throw new ModelEngineException(TOOLCALL_SIZE_ERROR, toolCalls.size(), MODEL_MESSAGE_AMOUNT);
        }
        
        // 5. 智能参数解析和验证
        ToolCall toolCall = toolCalls.get(0);
        String arguments = toolCall.arguments();
        
        log.debug("Raw tool call arguments for attempt {}: {}", attempt, arguments);
        
        try {
            Map<String, Object> parsedArgs = argumentsParser.parseArguments(arguments, tool);
            log.debug("Successfully parsed arguments for attempt {}: {}", attempt, parsedArgs);
            return parsedArgs;
            
        } catch (Exception e) {
            log.warn("Arguments parsing failed for attempt {}: {}", attempt, e.getMessage());
            throw new ModelEngineException(MODEL_RESPONSE_ERROR);
        }
    }
    
    /**
     * 构建增强的工具信息。
     */
    private ToolInfo buildEnhancedToolInfo(String outputSchema, String modelName, int attempt) {
        try {
            Map<String, Object> schemaMap = serializer.deserialize(outputSchema, Map.class);
            
            // 根据模型和尝试次数调整工具描述
            String baseDescription = "需要执行的函数";
            String enhancedDescription = enhanceToolDescription(baseDescription, modelName, attempt);
            
            return ToolInfo.custom()
                .name("request_tool")
                .description(enhancedDescription)
                .parameters(schemaMap)
                .build();
                
        } catch (SerializationException e) {
            log.warn("Failed to parse output schema, using fallback: {}", e.getMessage());
            
            // 创建默认的工具信息
            Map<String, Object> fallbackSchema = MapBuilder.<String, Object>get()
                .put("type", "object")
                .put("properties", new HashMap<>())
                .build();
                
            return ToolInfo.custom()
                .name("request_tool")
                .description("需要执行的函数")
                .parameters(fallbackSchema)
                .build();
        }
    }
    
    /**
     * 增强工具描述。
     */
    private String enhanceToolDescription(String baseDescription, String modelName, int attempt) {
        StringBuilder enhanced = new StringBuilder(baseDescription);
        
        if (attempt > 1) {
            enhanced.append("。这是第").append(attempt).append("次尝试");
        }
        
        // 根据模型添加特定指导
        if (modelName.toLowerCase().contains("qwen")) {
            enhanced.append("。请严格按照JSON格式要求输出，避免多余的逗号和引号嵌套");
        } else if (modelName.toLowerCase().contains("chatglm")) {
            enhanced.append("。请特别注意数组格式和中英文标点符号的使用");
        } else if (modelName.toLowerCase().contains("gpt")) {
            enhanced.append("。请按照OpenAI function calling标准格式输出");
        }
        
        enhanced.append("。确保参数完整性和类型正确性");
        
        return enhanced.toString();
    }
    
    /**
     * 构建模板变量。
     */
    private Map<String, Object> buildTemplateVariables(Map<String, String> variables) {
        Map<String, Object> templateVars = new HashMap<>();
        
        // 转换类型以匹配模板管理器的期望
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            templateVars.put(entry.getKey(), entry.getValue());
        }
        
        return templateVars;
    }
    
    /**
     * 增强聊天选项。
     */
    private ChatOption enhanceChatOption(ChatOption originalOption, ToolInfo tool, int attempt) {
        ChatOption.Builder builder = ChatOption.custom(originalOption)
            .stream(false)
            .tools(Collections.singletonList(tool));
        
        // 根据尝试次数调整参数
        if (attempt > 1) {
            // 降低温度以获得更稳定的输出
            Double currentTemp = originalOption.temperature();
            if (currentTemp != null && currentTemp > 0.1) {
                builder.temperature(Math.max(0.1, currentTemp - 0.1 * attempt));
            }
            
            // 增加最大 token 数以给模型更多输出空间
            Integer currentMaxTokens = originalOption.maxTokens();
            if (currentMaxTokens != null) {
                builder.maxTokens(Math.min(4096, currentMaxTokens + 100 * attempt));
            }
        }
        
        return builder.build();
    }
    
    /**
     * 为重试调整参数。
     */
    private void adjustForRetry(ChatOption chatOption, int attempt) {
        // 这里可以添加一些重试前的调整逻辑，比如短暂延迟
        try {
            Thread.sleep(100 * attempt); // 递增延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 