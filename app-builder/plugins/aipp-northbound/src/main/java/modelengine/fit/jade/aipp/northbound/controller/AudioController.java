/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.northbound.controller;

import static modelengine.fitframework.inspection.Validation.notNull;

import modelengine.fit.jane.task.gateway.Authenticator;
import modelengine.jade.voice.service.VoiceService;

import modelengine.fit.http.annotation.GetMapping;
import modelengine.fit.http.annotation.RequestBean;
import modelengine.fit.http.annotation.RequestMapping;
import modelengine.fit.jane.common.controller.AbstractController;
import modelengine.fit.jane.common.response.Rsp;
import modelengine.fit.jober.aipp.dto.chat.AudioTranslationParams;
import modelengine.fit.jober.aipp.dto.chat.TextTranslationParams;
import modelengine.fitframework.annotation.Component;

/**
 * 语音文字互转北向接口。
 *
 * @author 曹嘉美
 * @since 2024-12-17
 */
@Component
@RequestMapping(path = "/api/app/v1/tenants/{tenantId}/translation", group = "语音文字互转接口")
public class AudioController extends AbstractController {
    private final VoiceService voiceService;

    /**
     * 构造方法。
     *
     * @param authenticator 表示认证器的 {@link Authenticator}。
     * @param toolService 表示语音服务的 {@link VoiceService}。
     */
    public AudioController(Authenticator authenticator, VoiceService toolService) {
        super(authenticator);
        this.voiceService = notNull(toolService, "The voice service cannot be null.");
    }

    /**
     * 语音转文字。
     *
     * @param params 表示语音转文字参数的 {@link TextTranslationParams}。
     * @return 表示格式化之后的返回消息的 {@link Rsp}{@code <}{@link String}{@code >}。
     */
    @GetMapping(path = "/text", summary = "语音转文字", description = "该接口可以将输入的语音文件转换为文字。")
    public Rsp<String> audioToText(@RequestBean TextTranslationParams params) {
        return Rsp.ok(this.voiceService.getText(params.getVoicePath() + "&fileName=" + params.getFileName()));
    }

    /**
     * 文字转语音。
     *
     * @param params 表示文字转语音参数的 {@link AudioTranslationParams}。
     * @return 表示格式化之后的返回消息的 {@link Rsp}{@code <}{@link String}{@code >}。
     */
    @GetMapping(path = "/audio", summary = "文字转语音", description = "该接口可以将输入的文本转换为指定音色的语音。")
    public Rsp<String> textToAudio(@RequestBean AudioTranslationParams params) {
        return Rsp.ok(this.voiceService.getVoice(params.getText(), params.getTone()));
    }
}