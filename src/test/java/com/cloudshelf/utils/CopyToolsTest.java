package com.cloudshelf.utils;

import com.cloudshelf.entity.dto.SessionWebUserDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CopyTools 单元测试
 */
class CopyToolsTest {

    @Test
    void copy_shouldCopyProperties() {
        SessionWebUserDto src = new SessionWebUserDto();
        src.setUserId("USER001");
        src.setNickName("测试用户");
        src.setAdmin(true);

        SessionWebUserDto dest = CopyTools.copy(src, SessionWebUserDto.class);

        assertEquals("USER001", dest.getUserId());
        assertEquals("测试用户", dest.getNickName());
        assertTrue(dest.getAdmin());
    }

    @Test
    void copyList_shouldCopyAllItems() {
        SessionWebUserDto user1 = new SessionWebUserDto();
        user1.setUserId("U001");
        user1.setNickName("用户1");

        SessionWebUserDto user2 = new SessionWebUserDto();
        user2.setUserId("U002");
        user2.setNickName("用户2");

        List<SessionWebUserDto> srcList = new ArrayList<>();
        srcList.add(user1);
        srcList.add(user2);

        List<SessionWebUserDto> destList = CopyTools.copyList(srcList, SessionWebUserDto.class);

        assertEquals(2, destList.size());
        assertEquals("U001", destList.get(0).getUserId());
        assertEquals("U002", destList.get(1).getUserId());
    }

    @Test
    void copyList_shouldReturnEmptyList_whenSourceEmpty() {
        List<SessionWebUserDto> result = CopyTools.copyList(new ArrayList<>(), SessionWebUserDto.class);
        assertTrue(result.isEmpty());
    }
}
