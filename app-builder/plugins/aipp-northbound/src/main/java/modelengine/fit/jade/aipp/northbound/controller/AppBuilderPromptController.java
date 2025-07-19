/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.northbound.controller;

import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fit.jane.task.gateway.Authenticator;

import modelengine.fit.http.annotation.GetMapping;
import modelengine.fit.http.annotation.PathVariable;
import modelengine.fit.http.annotation.RequestMapping;
import modelengine.fit.http.server.HttpClassicServerRequest;
import modelengine.fit.jane.common.controller.AbstractController;
import modelengine.fit.jane.common.response.Rsp;
import modelengine.fit.jober.aipp.dto.chat.PromptCategory;
import modelengine.fit.jober.aipp.dto.chat.PromptInfo;
import modelengine.fit.jober.aipp.genericable.adapter.AppBuilderPromptServiceAdapter;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Property;

import java.util.List;

/**
 * 灵感大全管理北向接口。
 *
 * @author 曹嘉美
 * @since 2024-12-12
 */
@Component
@RequestMapping(path = "/api/app/v1/tenants/{tenantId}/apps/{appId}/prompt", group = "灵感大全管理接口")
public class AppBuilderPromptController extends AbstractController {
    private final AppBuilderPromptServiceAdapter service;

    /**
     * 构造函数。
     *
     * @param authenticator 表示身份校验器的 {@link Authenticator}。
     * @param service 表示灵感大全查询服务的 {@link AppBuilderPromptServiceAdapter}。
     */
    public AppBuilderPromptController(Authenticator authenticator, AppBuilderPromptServiceAdapter service) {
        super(authenticator);
        this.service = notNull(service, "The service cannot be null.");
    }

    /**
     * 查询所有的灵感类别。
     *
     * @param httpRequest 表示 HTTP 请求的 {@link HttpClassicServerRequest}。
     * @param tenantId 表示租户的唯一标识符的 {@link String}。
     * @param appId 表示应用的唯一标识符的 {@link String}。
     * @return 表示所有灵感类别的 {@link Rsp}{@code <}{@link List}{@code <}{@link String}{@code >}{@code >}。
     */
    @GetMapping(path = "/categories", summary = "获取灵感类别",
            description = "该接口可以通过应用的唯一标识符获取该应用下的所有灵感类别。")
    public Rsp<List<PromptCategory>> listCategories(HttpClassicServerRequest httpRequest,
            @PathVariable("tenantId") @Property(description = "租户的唯一标识符") String tenantId,
            @PathVariable("appId") @Property(description = "应用的唯一标识符") String appId) {
        return Rsp.ok(this.service.listPromptCategories(appId, this.contextOf(httpRequest, tenantId), false));
    }

    /**
     * 查询指定类别的所有灵感。
     *
     * @param httpRequest 表示 HTTP 请求的 {@link HttpClassicServerRequest}。
     * @param tenantId 表示租户的唯一标识符的 {@link String}。
     * @param appId 表示应用的唯一标识符的 {@link String}。
     * @param categoryId 表示灵感类别的唯一标识符的 {@link String}。
     * @return 表示灵感的
     * {@link Rsp}{@code <}{@link List}{@code <}{@link PromptInfo}{@code >>}。
     */
    @GetMapping(path = "/categories/{categoryId}/inspirations", summary = "获取灵感",
            description = "该接口可以获取指定应用的某一灵感类别下的所有灵感。")
    public Rsp<PromptInfo> queryInspirations(HttpClassicServerRequest httpRequest,
            @PathVariable("tenantId") @Property(description = "租户的唯一标识符") String tenantId,
            @PathVariable("appId") @Property(description = "应用的唯一标识符") String appId,
            @PathVariable("categoryId") @Property(description = "灵感类别的唯一标识符") String categoryId) {
        return Rsp.ok(this.service.queryInspirations(appId, categoryId, this.contextOf(httpRequest, tenantId), false));
    }
}
