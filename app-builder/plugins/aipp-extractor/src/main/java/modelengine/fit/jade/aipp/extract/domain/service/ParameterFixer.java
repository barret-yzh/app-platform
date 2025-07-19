/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import modelengine.fel.core.tool.ToolInfo;

import java.util.Map;

/**
 * 参数修复器接口，用于修复和清理工具调用参数。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
public interface ParameterFixer {
    
    /**
     * 清理JSON字符串。
     *
     * @param json 表示原始JSON字符串的 {@link String}。
     * @return 表示清理后JSON字符串的 {@link String}。
     */
    String cleanJson(String json);
    
    /**
     * 修复参数。
     *
     * @param arguments 表示原始参数的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示修复后参数的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     */
    Map<String, Object> fixParameters(Map<String, Object> arguments, ToolInfo toolInfo);
    
    /**
     * 智能修复参数，使用模糊匹配和智能转换。
     *
     * @param originalJson 表示原始JSON字符串的 {@link String}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 表示智能修复后参数的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     */
    Map<String, Object> intelligentFix(String originalJson, ToolInfo toolInfo);
    
    /**
     * 类型转换。
     *
     * @param value 表示原始值的 {@link String}。
     * @param paramSchema 表示参数Schema的 {@link Object}。
     * @return 表示转换后值的 {@link Object}。
     */
    Object convertValue(String value, Object paramSchema);
} 