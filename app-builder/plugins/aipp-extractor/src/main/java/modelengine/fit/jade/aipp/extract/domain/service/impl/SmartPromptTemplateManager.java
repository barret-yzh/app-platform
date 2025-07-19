/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service.impl;

import static modelengine.fitframework.inspection.Validation.notBlank;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fel.core.template.support.DefaultStringTemplate;
import modelengine.fel.core.tool.ToolInfo;
import modelengine.fit.jade.aipp.extract.domain.service.FunctionCallExampleGenerator;
import modelengine.fit.jade.aipp.extract.domain.service.PromptTemplateManager;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.log.Logger;
import modelengine.fitframework.serialization.ObjectSerializer;
import modelengine.fitframework.util.MapBuilder;
import modelengine.fitframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能提示词模板管理器实现，支持模型特定优化和动态示例生成。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class SmartPromptTemplateManager implements PromptTemplateManager {
    
    private static final Logger log = Logger.get(SmartPromptTemplateManager.class);
    
    /**
     * 基础提示词模板。
     */
    private static final String BASE_TEMPLATE = """
        你是一个精确的工具调用专家。请严格按照以下规则调用工具：
        
        ## 核心规则
        1. **严格遵循参数格式**：确保所有参数符合JSON Schema定义
        2. **类型准确性**：字符串用双引号，数字不用引号，布尔值用true/false  
        3. **必填参数检查**：确保所有required参数都有有效值
        4. **值的合理性**：参数值应符合业务逻辑，避免无意义的值
        5. **JSON格式严格**：避免多余的逗号、错误的引号嵌套
        
        ## 工具信息
        工具名称：{{toolName}}
        工具描述：{{toolDescription}}
        参数结构：
        ```json
        {{toolParameters}}
        ```
        
        {{modelSpecificPrompt}}
        
        {{examples}}
        
        ## 输入内容
        {{inputContent}}
        
        请调用工具 {{toolName}}，确保参数格式完全正确。
        """;
    
    /**
     * 模型特定的优化提示词。
     */
    private final Map<String, String> modelSpecificPrompts;
    
    private final FunctionCallExampleGenerator exampleGenerator;
    private final ObjectSerializer serializer;
    
    /**
     * 构造函数。
     *
     * @param exampleGenerator 表示示例生成器的 {@link FunctionCallExampleGenerator}。
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     */
    public SmartPromptTemplateManager(FunctionCallExampleGenerator exampleGenerator, ObjectSerializer serializer) {
        this.exampleGenerator = notNull(exampleGenerator, "The example generator cannot be null.");
        this.serializer = notNull(serializer, "The serializer cannot be null.");
        this.modelSpecificPrompts = initializeModelSpecificPrompts();
    }
    
    @Override
    public String getFunctionCallingPrompt(String modelName, ToolInfo toolInfo, Map<String, Object> variables) {
        notBlank(modelName, "The model name cannot be blank.");
        notNull(toolInfo, "The tool info cannot be null.");
        notNull(variables, "The variables cannot be null.");
        
        try {
            // 构建模板变量
            Map<String, Object> templateVars = buildTemplateVariables(modelName, toolInfo, variables);
            
            // 渲染模板
            DefaultStringTemplate template = new DefaultStringTemplate(BASE_TEMPLATE);
            String prompt = template.render(templateVars);
            
            log.debug("Generated function calling prompt for model [{}], tool [{}]", modelName, toolInfo.name());
            return prompt;
            
        } catch (Exception e) {
            log.error("Failed to generate function calling prompt for model [{}], tool [{}]: {}", 
                modelName, toolInfo.name(), e.getMessage());
            throw new IllegalStateException("Failed to generate function calling prompt", e);
        }
    }
    
    @Override
    public String getBaseTemplate() {
        return BASE_TEMPLATE;
    }
    
    @Override
    public String getModelSpecificPrompt(String modelName) {
        if (StringUtils.isBlank(modelName)) {
            return "";
        }
        
        return modelSpecificPrompts.entrySet().stream()
            .filter(entry -> modelName.toLowerCase().contains(entry.getKey().toLowerCase()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("");
    }
    
    /**
     * 构建模板变量。
     *
     * @param modelName 模型名称
     * @param toolInfo 工具信息
     * @param variables 输入变量
     * @return 模板变量映射
     */
    private Map<String, Object> buildTemplateVariables(String modelName, ToolInfo toolInfo, Map<String, Object> variables) {
        Map<String, Object> templateVars = new HashMap<>();
        
        // 基本工具信息
        templateVars.put("toolName", toolInfo.name());
        templateVars.put("toolDescription", toolInfo.description());
        templateVars.put("toolParameters", serializer.serialize(toolInfo.parameters()));
        
        // 模型特定提示
        String modelPrompt = getModelSpecificPrompt(modelName);
        templateVars.put("modelSpecificPrompt", StringUtils.isBlank(modelPrompt) ? "" : 
            "\n## 特别注意\n" + modelPrompt + "\n");
        
        // 生成示例
        List<String> examples = exampleGenerator.generateExamples(toolInfo);
        templateVars.put("examples", examples.isEmpty() ? "" : 
            "\n## 调用示例\n" + String.join("\n\n", examples) + "\n");
        
        // 构建输入内容
        String inputContent = buildInputContent(variables);
        templateVars.put("inputContent", inputContent);
        
        return templateVars;
    }
    
    /**
     * 构建输入内容部分。
     *
     * @param variables 输入变量
     * @return 格式化的输入内容
     */
    private String buildInputContent(Map<String, Object> variables) {
        StringBuilder content = new StringBuilder();
        
        String histories = (String) variables.getOrDefault("histories", "");
        String text = (String) variables.getOrDefault("text", "");
        String desc = (String) variables.getOrDefault("desc", "");
        
        if (StringUtils.isNotBlank(histories)) {
            content.append("对话历史记录：\n```\n").append(histories).append("\n```\n\n");
        }
        
        if (StringUtils.isNotBlank(desc)) {
            content.append("提取要求：\n").append(desc).append("\n\n");
        }
        
        if (StringUtils.isNotBlank(text)) {
            content.append("本次输入内容：\n").append(text);
        }
        
        return content.toString();
    }
    
    /**
     * 初始化模型特定提示词。
     *
     * @return 模型特定提示词映射
     */
    private Map<String, String> initializeModelSpecificPrompts() {
        return MapBuilder.<String, String>get()
            .put("qwen", "Qwen模型特别提醒：请确保JSON格式严格正确，避免多余的逗号和引号嵌套。" +
                "数组使用[]，对象使用{}，字符串必须用双引号包围。")
            .put("chatglm", "ChatGLM模型特别提醒：请特别注意数组格式，使用[]包围数组元素。" +
                "避免使用中文标点符号，所有符号使用英文格式。")
            .put("gpt", "GPT模型提醒：请按照OpenAI function calling标准格式输出，" +
                "确保参数完整性和类型正确性。")
            .put("baichuan", "百川模型提醒：请严格按照JSON Schema格式要求，" +
                "确保所有必填字段都有合适的值。")
            .put("yi", "Yi模型提醒：请仔细检查参数类型，确保字符串、数字、布尔值格式正确。")
            .build();
    }
} 