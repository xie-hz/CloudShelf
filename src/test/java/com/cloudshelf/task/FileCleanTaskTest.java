package com.cloudshelf.task;

import com.cloudshelf.entity.enums.FileDelFlagEnums;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.service.FileInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileCleanTask 单元测试
 * 覆盖: 回收站过期文件清理 / 空回收站 / 多用户分组删除
 */
@ExtendWith(MockitoExtension.class)
class FileCleanTaskTest {

    @Mock
    private FileInfoService fileInfoService;

    @InjectMocks
    private FileCleanTask fileCleanTask;

    @Test
    void execute_shouldCleanExpiredFiles() {
        FileInfo expiredFile = new FileInfo();
        expiredFile.setFileId("FILE001");
        expiredFile.setUserId("USER001");

        when(fileInfoService.findListByParam(any())).thenReturn(Collections.singletonList(expiredFile));

        fileCleanTask.execute();

        verify(fileInfoService).delFileBatch(eq("USER001"), eq("FILE001"), eq(false));
    }

    @Test
    void execute_shouldGroupByUser() {
        FileInfo file1 = new FileInfo();
        file1.setFileId("F001"); file1.setUserId("U_A");
        FileInfo file2 = new FileInfo();
        file2.setFileId("F002"); file2.setUserId("U_A");
        FileInfo file3 = new FileInfo();
        file3.setFileId("F003"); file3.setUserId("U_B");

        when(fileInfoService.findListByParam(any())).thenReturn(Arrays.asList(file1, file2, file3));

        fileCleanTask.execute();

        // U_A 的两个文件合并成一次调用
        verify(fileInfoService).delFileBatch(eq("U_A"), eq("F001,F002"), eq(false));
        // U_B 的文件单独一次调用
        verify(fileInfoService).delFileBatch(eq("U_B"), eq("F003"), eq(false));
    }

    @Test
    void execute_shouldNotDelete_whenNoExpiredFiles() {
        when(fileInfoService.findListByParam(any())).thenReturn(Collections.emptyList());

        fileCleanTask.execute();

        verify(fileInfoService, never()).delFileBatch(anyString(), anyString(), anyBoolean());
    }
}
