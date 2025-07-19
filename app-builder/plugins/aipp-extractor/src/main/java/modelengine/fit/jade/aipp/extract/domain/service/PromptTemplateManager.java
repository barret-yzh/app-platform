/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import modelengine.fel.core.tool.ToolInfo;

import java.util.Map;

/**
 * 提示词模板管理器接口，用于生成针对不同模型优化的 function calling 提示词。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
public interface PromptTemplateManager {
    
    /**
     * 获取针对特定模型的 function calling 提示词。
     *
     * @param modelName 表示模型名称的 {@link String}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @param variables 表示模板变量的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @return 表示优化后提示词的 {@link String}。
     * @throws IllegalArgumentException 当参数为 {@code null} 时。
     */
    String getFunctionCallingPrompt(String modelName, ToolInfo toolInfo, Map<String, Object> variables);
    
    /**
     * 获取基础提示词模板。
     *
     * @return 表示基础模板的 {@link String}。
     */
    String getBaseTemplate();
    
    /**
     * 获取模型特定的优化提示。
     *
     * @param modelName 表示模型名称的 {@link String}。
     * @return 表示模型特定提示的 {@link String}。
     */
    String getModelSpecificPrompt(String modelName);
} 