/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.domain.service;

import modelengine.fel.core.tool.ToolInfo;

import java.util.Map;

/**
 * 参数验证器接口，用于验证工具调用参数的有效性。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
public interface ParameterValidator {
    
    /**
     * 验证参数是否有效。
     *
     * @param arguments 表示参数映射的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 当参数有效时返回 {@code true}，否则返回 {@code false}。
     */
    boolean isValid(Map<String, Object> arguments, ToolInfo toolInfo);
    
    /**
     * 检查必填字段。
     *
     * @param arguments 表示参数映射的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 当必填字段都存在时返回 {@code true}，否则返回 {@code false}。
     */
    boolean checkRequiredFields(Map<String, Object> arguments, ToolInfo toolInfo);
    
    /**
     * 检查参数类型。
     *
     * @param arguments 表示参数映射的 {@link Map}{@code <}{@link String}{@code , }{@link Object}{@code >}。
     * @param toolInfo 表示工具信息的 {@link ToolInfo}。
     * @return 当参数类型正确时返回 {@code true}，否则返回 {@code false}。
     */
    boolean checkParameterTypes(Map<String, Object> arguments, ToolInfo toolInfo);
} 