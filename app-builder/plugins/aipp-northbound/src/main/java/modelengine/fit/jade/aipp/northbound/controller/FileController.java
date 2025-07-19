/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.northbound.controller;

import static modelengine.fitframework.inspection.Validation.notEmpty;
import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fit.jane.task.gateway.Authenticator;

import modelengine.jade.service.annotations.CarverSpan;
import modelengine.fit.http.annotation.PathVariable;
import modelengine.fit.http.annotation.PostMapping;
import modelengine.fit.http.annotation.RequestMapping;
import modelengine.fit.http.annotation.RequestQuery;
import modelengine.fit.http.annotation.ResponseStatus;
import modelengine.fit.http.entity.NamedEntity;
import modelengine.fit.http.entity.PartitionedEntity;
import modelengine.fit.http.protocol.HttpResponseStatus;
import modelengine.fit.http.server.HttpClassicServerRequest;
import modelengine.fit.jane.common.controller.AbstractController;
import modelengine.fit.jane.common.entity.OperationContext;
import modelengine.fit.jane.common.response.Rsp;
import modelengine.fit.jober.aipp.common.exception.AippErrCode;
import modelengine.fit.jober.aipp.common.exception.AippException;
import modelengine.fit.jober.aipp.dto.chat.FileUploadInfo;
import modelengine.fit.jober.aipp.genericable.adapter.FileServiceAdapter;
import modelengine.fitframework.annotation.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传北向接口。
 *
 * @author 曹嘉美
 * @since 2024-12-16
 */
@Component
@RequestMapping(path = "/api/app/v1/tenants/{tenantId}/file", group = "文件上传接口")
public class FileController extends AbstractController {
    private final FileServiceAdapter fileService;

    /**
     * 构造函数。
     *
     * @param authenticator 表示身份校验器的 {@link Authenticator}。
     * @param fileService 表示文件服务的 {@link FileServiceAdapter}。
     */
    public FileController(Authenticator authenticator, FileServiceAdapter fileService) {
        super(authenticator);
        this.fileService = notNull(fileService, "The fileService cannot be null.");
    }

    /**
     * 上传文件。
     *
     * @param httpRequest 表示 Http 请求体的 {@link HttpClassicServerRequest}。
     * @param tenantId 表示租户的唯一标识符的 {@link String}。
     * @param receivedFile 表示接收到的文件的 {@link PartitionedEntity}。
     * @return 返回响应结果的 {@link Rsp}{@code <}{@link FileUploadInfo}{@code >}。
     * @throws IOException 当发生 I/O 异常时。
     */
    @CarverSpan(value = "operation.file.upload")
    @PostMapping(summary = "上传文件", description = "该接口可以往指定应用上传文件。")
    @ResponseStatus(HttpResponseStatus.CREATED)
    public Rsp<FileUploadInfo> uploadFile(HttpClassicServerRequest httpRequest,
            @PathVariable("tenantId") String tenantId, @RequestQuery(value = "app_id", required = true) String appId,
            PartitionedEntity receivedFile) throws IOException {
        List<NamedEntity> entityList =
                receivedFile.entities().stream().filter(NamedEntity::isFile).collect(Collectors.toList());
        notEmpty(entityList, () -> new AippException(AippErrCode.UPLOAD_FAILED));
        String fileName = receivedFile.entities().get(0).name();
        OperationContext context = this.contextOf(httpRequest, tenantId);
        FileUploadInfo fileRspDto = this.fileService.uploadFile(context, tenantId, fileName, appId, receivedFile);
        return Rsp.ok(fileRspDto);
    }
}
