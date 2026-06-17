package com.cloudshelf.utils;

import com.cloudshelf.entity.enums.VerifyRegexEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VerifyUtils 单元测试
 */
class VerifyUtilsTest {

    @Test
    void verify_shouldReturnTrue_whenMatches() {
        assertTrue(VerifyUtils.verify("\\d+", "12345"));
        assertTrue(VerifyUtils.verify("[a-z]+", "hello"));
    }

    @Test
    void verify_shouldReturnFalse_whenNotMatches() {
        assertFalse(VerifyUtils.verify("\\d+", "abc"));
    }

    @Test
    void verify_shouldReturnFalse_whenValueEmpty() {
        assertFalse(VerifyUtils.verify("\\d+", ""));
        assertFalse(VerifyUtils.verify("\\d+", (String) null));
    }

    @Test
    void verifyWithEnum_shouldUseEnumRegex() {
        // VerifyRegexEnum.EMAIL 的正则是邮箱格式
        assertTrue(VerifyUtils.verify(VerifyRegexEnum.EMAIL, "test@qq.com"));
        assertFalse(VerifyUtils.verify(VerifyRegexEnum.EMAIL, "not-an-email"));

        // VerifyRegexEnum.PASSWORD 要求 8-18 位字母数字特殊字符
        assertTrue(VerifyUtils.verify(VerifyRegexEnum.PASSWORD, "MyPass123!"));
        assertFalse(VerifyUtils.verify(VerifyRegexEnum.PASSWORD, "123"));
    }
}
