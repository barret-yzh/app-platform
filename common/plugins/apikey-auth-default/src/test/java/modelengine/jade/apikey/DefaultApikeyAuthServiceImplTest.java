/*
 * Copyright (c) 2025 Huawei Technologies Co., Ltd. All rights reserved.
 * This file is a part of the ModelEngine Project.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package modelengine.jade.apikey;

import modelengine.jade.apikey.impl.DefaultApikeyAuthServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 表示 {@link DefaultApikeyAuthServiceImpl} 的测试类。
 *
 * @author 陈潇文
 * @since 2025-07-07
 */
public class DefaultApikeyAuthServiceImplTest {
    @Test
    void testAuthApikeyInfo() {
        DefaultApikeyAuthServiceImpl service = new DefaultApikeyAuthServiceImpl();
        assertThat(service.authApikeyInfo(null)).isTrue();
        assertThat(service.authApikeyInfo("")).isTrue();
        assertThat(service.authApikeyInfo("any-string")).isTrue();
        assertThat(service.authApikeyInfo("Bearer ME-sk-1234567890abcdef-abcdef1234567890abcdef1234567890")).isTrue();
    }
}
