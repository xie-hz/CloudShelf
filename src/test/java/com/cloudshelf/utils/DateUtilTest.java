package com.cloudshelf.utils;

import com.cloudshelf.entity.enums.DateTimePatternEnum;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtil 单元测试
 */
class DateUtilTest {

    @Test
    void format_shouldReturnFormattedDate() {
        Date date = new Date(2023 - 1900, 0, 15, 10, 30, 0);  // 2023-01-15
        String result = DateUtil.format(date, "yyyy-MM-dd");
        assertEquals("2023-01-15", result);
    }

    @Test
    void format_shouldUseDateTimePatternEnum() {
        Date date = new Date(2023 - 1900, 5, 10, 14, 0, 0);
        String result = DateUtil.format(date, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern());
        assertEquals("2023-06-10 14:00:00", result);
    }

    @Test
    void parse_shouldReturnDate() {
        Date result = DateUtil.parse("2023-06-15", "yyyy-MM-dd");
        assertNotNull(result);
    }

    @Test
    void getAfterDate_shouldReturnFutureDate() {
        Date now = new Date();
        Date after7Days = DateUtil.getAfterDate(7);
        long diff = after7Days.getTime() - now.getTime();
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        // 允许 1 秒误差
        assertTrue(Math.abs(diff - sevenDaysMs) < 1000);
    }

    @Test
    void getAfterDate_shouldReturnPastDate_whenNegative() {
        Date now = new Date();
        Date before7Days = DateUtil.getAfterDate(-7);
        assertTrue(before7Days.before(now));
    }
}
