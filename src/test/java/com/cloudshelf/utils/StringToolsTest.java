package com.cloudshelf.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringTools 单元测试
 */
class StringToolsTest {

    // ==================== isEmpty ====================

    @Test
    void isEmpty_shouldReturnTrue_whenNull() {
        assertTrue(StringTools.isEmpty(null));
    }

    @Test
    void isEmpty_shouldReturnTrue_whenEmptyString() {
        assertTrue(StringTools.isEmpty(""));
    }

    @Test
    void isEmpty_shouldReturnTrue_whenNullString() {
        assertTrue(StringTools.isEmpty("null"));
    }

    @Test
    void isEmpty_shouldReturnTrue_whenWhitespaceOnly() {
        assertTrue(StringTools.isEmpty("   "));
    }

    @Test
    void isEmpty_shouldReturnFalse_whenNonEmpty() {
        assertFalse(StringTools.isEmpty("hello"));
    }

    // ==================== getFileSuffix ====================

    @Test
    void getFileSuffix_shouldReturnSuffix() {
        assertEquals(".txt", StringTools.getFileSuffix("test.txt"));
        assertEquals(".mp4", StringTools.getFileSuffix("video.mp4"));
        assertEquals(".gz", StringTools.getFileSuffix("archive.tar.gz"));  // lastIndexOf 取最后一个 .
    }

    @Test
    void getFileSuffix_shouldReturnEmpty_whenNoDot() {
        assertEquals("", StringTools.getFileSuffix("noextension"));
    }

    // ==================== getFileNameNoSuffix ====================

    @Test
    void getFileNameNoSuffix_shouldStripExtension() {
        assertEquals("test", StringTools.getFileNameNoSuffix("test.txt"));
        assertEquals("archive.tar", StringTools.getFileNameNoSuffix("archive.tar.gz"));  // lastIndexOf
    }

    @Test
    void getFileNameNoSuffix_shouldReturnOriginal_whenNoExtension() {
        assertEquals("noextension", StringTools.getFileNameNoSuffix("noextension"));
    }

    // ==================== rename ====================

    @Test
    void rename_shouldAddRandomSuffix() {
        String result = StringTools.rename("test.txt");
        assertTrue(result.startsWith("test_"));
        assertTrue(result.endsWith(".txt"));
        assertTrue(result.length() > "test.txt".length());
    }

    // ==================== getRandomString / getRandomNumber ====================

    @Test
    void getRandomString_shouldReturnCorrectLength() {
        assertEquals(10, StringTools.getRandomString(10).length());
        assertEquals(5, StringTools.getRandomString(5).length());
    }

    @Test
    void getRandomNumber_shouldReturnNumericOnly() {
        String num = StringTools.getRandomNumber(8);
        assertEquals(8, num.length());
        assertTrue(num.matches("\\d+"));
    }

    // ==================== escapeTitle / escapeHtml ====================

    @Test
    void escapeTitle_shouldEscapeAngleBrackets() {
        assertEquals("&lt;script>", StringTools.escapeTitle("<script>"));  // 只转义 < 不转义 >
    }

    @Test
    void escapeHtml_shouldEscapeMultipleChars() {
        String result = StringTools.escapeHtml("<a href='x'>\nclick</a>");
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&nbsp;"));
        assertTrue(result.contains("<br>"));
    }

    @Test
    void escapeHtml_shouldReturnOriginal_whenEmpty() {
        assertNull(StringTools.escapeHtml(null));
        assertEquals("", StringTools.escapeHtml(""));
    }

    // ==================== pathIsOk ====================

    @Test
    void pathIsOk_shouldReturnTrue_whenEmptyOrNull() {
        assertTrue(StringTools.pathIsOk(null));
        assertTrue(StringTools.pathIsOk(""));
    }

    @Test
    void pathIsOk_shouldReturnTrue_whenNormalPath() {
        assertTrue(StringTools.pathIsOk("D:/files/test.txt"));
        assertTrue(StringTools.pathIsOk("/var/log/app.log"));
    }

    @Test
    void pathIsOk_shouldReturnFalse_whenPathTraversal() {
        assertFalse(StringTools.pathIsOk("../../../etc/passwd"));
        assertFalse(StringTools.pathIsOk("..\\..\\windows\\system32"));
    }

    // ==================== encodeByMD5 ====================

    @Test
    void encodeByMD5_shouldReturnNull_whenEmpty() {
        assertNull(StringTools.encodeByMD5(null));
        assertNull(StringTools.encodeByMD5(""));
    }

    @Test
    void encodeByMD5_shouldReturn32CharHex() {
        String hash = StringTools.encodeByMD5("hello");
        assertNotNull(hash);
        assertEquals(32, hash.length());
        assertTrue(hash.matches("[a-f0-9]+"));
    }

    @Test
    void encodeByMD5_shouldBeDeterministic() {
        assertEquals(StringTools.encodeByMD5("test"), StringTools.encodeByMD5("test"));
    }
}
