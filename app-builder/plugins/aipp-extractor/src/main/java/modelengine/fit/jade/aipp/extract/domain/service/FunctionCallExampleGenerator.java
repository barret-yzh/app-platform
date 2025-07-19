/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import modelengine.fel.core.tool.ToolInfo;

import java.util.List;

/**
 * function calling 示例生成器接口，用于根据工具Schema动态生成调用示例。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
public interface FunctionCallExampleGenerator {
    
    /**
     * 根据工具信息生成调用示例。
     *
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示调用示例列表的 {@link List}{@code <}{@link String}{@code >}。
     * @throws IllegalArgumentException 当 {@code toolInfo} 为 {@code null} 时。
     */
    List<String> generateExamples(ToolInfo toolInfo);
    
    /**
     * 生成正确的调用示例。
     *
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示正确示例的 {@link String}。
     */
    String generateCorrectExample(ToolInfo toolInfo);
    
    /**
     * 生成错误避免示例。
     *
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示错误避免示例的 {@link String}。
     */
    String generateErrorAvoidanceExample(ToolInfo toolInfo);
} 