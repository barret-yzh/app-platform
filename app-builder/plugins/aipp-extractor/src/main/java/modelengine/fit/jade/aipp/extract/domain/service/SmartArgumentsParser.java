/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import modelengine.fel.core.tool.ToolInfo;

import java.util.Map;

/**
 * 智能参数解析器接口，支持多次尝试和自动修复的参数解析。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
public interface SmartArgumentsParser {
    
    /**
     * 智能解析工具调用参数，支持多次尝试和自动修复。
     *
     * @param argumentsJson 表示参数JSON字符串的 {@link String}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示解析后参数的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @throws IllegalArgumentException 当参数为 {@code null} 时。
     * @throws IllegalStateException 当所有解析尝试都失败时。
     */
    Map<String, Object> parseArguments(String argumentsJson, ToolInfo toolInfo);
    
    /**
     * 直接解析参数，不进行修复。
     *
     * @param argumentsJson 表示参数JSON字符串的 {@link String}。
     * @return 表示解析后参数的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @throws Exception 当解析失败时。
     */
    Map<String, Object> directParse(String argumentsJson) throws Exception;
} 