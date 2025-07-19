/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.jade.common.filter.support;

import modelengine.fit.http.protocol.HttpResponseStatus;
import modelengine.fit.http.server.HttpClassicServerRequest;
import modelengine.fit.http.server.HttpClassicServerResponse;
import modelengine.fit.http.server.HttpServerFilter;
import modelengine.fit.http.server.HttpServerFilterChain;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Order;
import modelengine.fitframework.annotation.Scope;
import modelengine.fitframework.inspection.Validation;
import modelengine.fitframework.log.Logger;
import modelengine.jade.apikey.ApikeyAuthService;
import modelengine.jade.authentication.context.HttpRequestUtils;
import modelengine.jade.authentication.context.UserContext;
import modelengine.jade.authentication.context.UserContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 用于北向接口的过滤器类。
 *
 * @author 陈潇文
 * @since 2025-07-07
 */
@Component
public class NorthFilter implements HttpServerFilter {
    private static final Logger log = Logger.get(NorthFilter.class);
    private static final String USER_NAME_PREFIX = "sys_api_";
    private static final int ME_SK_START_POS = 13;
    private static final int ME_SK_END_POS = 21;

    private final ApikeyAuthService apikeyAuthService;

    /**
     * 用 apikey 鉴权服务 {@link ApikeyAuthService} 构造 {@link NorthFilter}。
     *
     * @param apikeyAuthService 表示 apikey 鉴权服务的 {@link ApikeyAuthService}。
     */
    public NorthFilter(ApikeyAuthService apikeyAuthService) {
        this.apikeyAuthService = Validation.notNull(apikeyAuthService, "The auth service cannot be null.");
    }

    @Override
    public String name() {
        return "NorthFilter";
    }

    @Override
    public int priority() {
        return Order.HIGHEST;
    }

    @Override
    public List<String> matchPatterns() {
        return Collections.singletonList("/api/app/v1/**");
    }

    @Override
    public List<String> mismatchPatterns() {
        return Collections.emptyList();
    }

    @Override
    public void doFilter(HttpClassicServerRequest request, HttpClassicServerResponse response,
            HttpServerFilterChain chain) {
        Optional<String> token = request.headers().first("Authorization");
        log.info("Received request with Authorization token.");

        if (token.isEmpty() || !this.apikeyAuthService.authApikeyInfo(token.get())) {
            // 认证失败，返回 401 错误
            response.statusCode(HttpResponseStatus.UNAUTHORIZED.statusCode());
            log.error("Authentication failed: Token is null or invalid.");
            response.send();
            return;
        }

        String userName = this.generateUniqueNameForApiKey(token.get());

        UserContext operationContext = new UserContext(userName,
                HttpRequestUtils.getUserIp(request),
                HttpRequestUtils.getAcceptLanguages(request));
        UserContextHolder.apply(operationContext, () -> chain.doFilter(request, response));
    }

    @Override
    public Scope scope() {
        return Scope.GLOBAL;
    }

    private String generateUniqueNameForApiKey(String apiKey) {
        return USER_NAME_PREFIX + apiKey.substring(ME_SK_START_POS, ME_SK_END_POS);
    }
}
