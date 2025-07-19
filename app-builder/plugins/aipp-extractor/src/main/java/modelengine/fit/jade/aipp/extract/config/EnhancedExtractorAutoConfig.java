/*---------------------------------------------------------------------------------------------
 *  Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 *  This file is a part of the ModelEngine Project.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

package modelengine.fit.jade.aipp.extract.config;

import modelengine.fel.core.chat.ChatModel;
import modelengine.fit.jade.aipp.extract.domain.entity.ContentExtractor;
import modelengine.fit.jade.aipp.extract.domain.entity.support.EnhancedFunctionCallContentExtractor;
import modelengine.fit.jade.aipp.extract.domain.service.FunctionCallExampleGenerator;
import modelengine.fit.jade.aipp.extract.domain.service.ParameterFixer;
import modelengine.fit.jade.aipp.extract.domain.service.ParameterValidator;
import modelengine.fit.jade.aipp.extract.domain.service.PromptTemplateManager;
import modelengine.fit.jade.aipp.extract.domain.service.SmartArgumentsParser;
import modelengine.fit.jade.aipp.extract.domain.service.impl.DefaultFunctionCallExampleGenerator;
import modelengine.fit.jade.aipp.extract.domain.service.impl.DefaultParameterFixer;
import modelengine.fit.jade.aipp.extract.domain.service.impl.DefaultParameterValidator;
import modelengine.fit.jade.aipp.extract.domain.service.impl.DefaultSmartArgumentsParser;
import modelengine.fit.jade.aipp.extract.domain.service.impl.SmartPromptTemplateManager;
import modelengine.fitframework.annotation.Bean;
import modelengine.fitframework.annotation.Component;
import modelengine.fitframework.annotation.Fit;
import modelengine.fitframework.annotation.Primary;
import modelengine.fitframework.serialization.ObjectSerializer;

/**
 * 增强版提取器自动配置类，用于配置和注册所有增强组件。
 *
 * @author AI Assistant
 * @since 2025-01-21
 */
@Component
public class EnhancedExtractorAutoConfig {
    
    /**
     * 配置参数验证器。
     *
     * @return 表示参数验证器的 {@link ParameterValidator}。
     */
    @Bean
    public ParameterValidator parameterValidator() {
        return new DefaultParameterValidator();
    }
    
    /**
     * 配置参数修复器。
     *
     * @return 表示参数修复器的 {@link ParameterFixer}。
     */
    @Bean
    public ParameterFixer parameterFixer() {
        return new DefaultParameterFixer();
    }
    
    /**
     * 配置示例生成器。
     *
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @return 表示示例生成器的 {@link FunctionCallExampleGenerator}。
     */
    @Bean
    public FunctionCallExampleGenerator functionCallExampleGenerator(@Fit(alias = "json") ObjectSerializer serializer) {
        return new DefaultFunctionCallExampleGenerator(serializer);
    }
    
    /**
     * 配置智能参数解析器。
     *
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @param validator 表示参数验证器的 {@link ParameterValidator}。
     * @param fixer 表示参数修复器的 {@link ParameterFixer}。
     * @return 表示智能参数解析器的 {@link SmartArgumentsParser}。
     */
    @Bean
    public SmartArgumentsParser smartArgumentsParser(@Fit(alias = "json") ObjectSerializer serializer,
                                                    ParameterValidator validator,
                                                    ParameterFixer fixer) {
        return new DefaultSmartArgumentsParser(serializer, validator, fixer);
    }
    
    /**
     * 配置提示词模板管理器。
     *
     * @param exampleGenerator 表示示例生成器的 {@link FunctionCallExampleGenerator}。
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @return 表示提示词模板管理器的 {@link PromptTemplateManager}。
     */
    @Bean
    public PromptTemplateManager promptTemplateManager(FunctionCallExampleGenerator exampleGenerator,
                                                      @Fit(alias = "json") ObjectSerializer serializer) {
        return new SmartPromptTemplateManager(exampleGenerator, serializer);
    }
    
    /**
     * 配置增强版内容提取器。
     * 使用 @Primary 注解确保优先使用增强版。
     *
     * @param modelService 表示模型服务的 {@link ChatModel}。
     * @param promptTemplateManager 表示提示词模板管理器的 {@link PromptTemplateManager}。
     * @param argumentsParser 表示智能参数解析器的 {@link SmartArgumentsParser}。
     * @param serializer 表示序列化器的 {@link ObjectSerializer}。
     * @return 表示增强版内容提取器的 {@link ContentExtractor}。
     */
    @Bean
    @Primary
    public ContentExtractor enhancedContentExtractor(ChatModel modelService,
                                                    PromptTemplateManager promptTemplateManager,
                                                    SmartArgumentsParser argumentsParser,
                                                    @Fit(alias = "json") ObjectSerializer serializer) {
        return new EnhancedFunctionCallContentExtractor(modelService, promptTemplateManager, argumentsParser, serializer);
    }
} 