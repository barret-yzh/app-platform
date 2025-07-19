/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jober.aipp.util;

import modelengine.fitframework.inspection.Validation;

/**
 * 大模型上下文协议相关工具方法。
 *
 * @author 宋永坦
 * @since 2025-07-11
 */
public class McpUtils {
    private static final String SSE_ENDPOINT_SPLIT_DELIMITER = "/";

    /**
     * 获取 {@code baseUrl} 部分。
     *
     * @param url 目标地址。
     * @return {@code baseUrl} 部分。
     * @throws IllegalArgumentException 当目标地址不包含 {@code sseEndpoint} 时。
     */
    public static String getBaseUrl(String url) {
        String[] splits = url.split(SSE_ENDPOINT_SPLIT_DELIMITER);
        Validation.greaterThan(splits.length, 3, "The url is wrong. [url={0}]", url);
        return url.substring(0, url.length() - splits[splits.length - 1].length() - 1);
    }

    /**
     * 获取 {@code sseEndpoint} 部分。
     *
     * @param url 目标地址。
     * @return {@code sseEndpoint} 部分。
     * @throws IllegalArgumentException 当目标地址不包含 {@code sseEndpoint} 时。
     */
    public static String getSseEndpoint(String url) {
        String[] splits = url.split(SSE_ENDPOINT_SPLIT_DELIMITER);
        Validation.greaterThan(splits.length, 3, "The url is wrong. [url={0}]", url);
        return SSE_ENDPOINT_SPLIT_DELIMITER + splits[splits.length - 1];
    }
}
