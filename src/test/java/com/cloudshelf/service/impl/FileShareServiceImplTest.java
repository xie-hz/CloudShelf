package com.cloudshelf.service.impl;

import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionShareDto;
import com.cloudshelf.entity.enums.ResponseCodeEnum;
import com.cloudshelf.entity.enums.ShareValidTypeEnums;
import com.cloudshelf.entity.po.FileShare;
import com.cloudshelf.entity.query.FileShareQuery;
import com.cloudshelf.exception.BusinessException;
import com.cloudshelf.mappers.FileShareMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FileShareServiceImpl 单元测试
 * 覆盖: 创建分享/取消分享/校验提取码
 */
@ExtendWith(MockitoExtension.class)
class FileShareServiceImplTest {

    @Mock
    private FileShareMapper<FileShare, FileShareQuery> fileShareMapper;

    @InjectMocks
    private FileShareServiceImpl fileShareService;

    // ==================== 创建分享 ====================

    @Test
    void saveShare_shouldCreateShare_withAllFields() {
        FileShare share = new FileShare();
        share.setFileId("FILE001");
        share.setUserId("USER001");
        share.setValidType(ShareValidTypeEnums.DAY_1.getType());
        // 不传 code，自动生成
        // 不传 shareId，自动生成

        when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

        fileShareService.saveShare(share);

        ArgumentCaptor<FileShare> captor = ArgumentCaptor.forClass(FileShare.class);
        verify(fileShareMapper).insert(captor.capture());
        FileShare saved = captor.getValue();

        assertEquals("FILE001", saved.getFileId());
        assertEquals("USER001", saved.getUserId());
        assertNotNull(saved.getShareId());
        assertEquals(20, saved.getShareId().length());
        assertNotNull(saved.getCode());
        assertEquals(5, saved.getCode().length());
        assertNotNull(saved.getShareTime());
        assertNotNull(saved.getExpireTime());  // 非永久有效
    }

    @Test
    void saveShare_shouldNotSetExpireTime_whenForever() {
        FileShare share = new FileShare();
        share.setFileId("FILE001");
        share.setUserId("USER001");
        share.setValidType(ShareValidTypeEnums.FOREVER.getType());
        share.setCode("ABCDE");

        when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

        fileShareService.saveShare(share);

        ArgumentCaptor<FileShare> captor = ArgumentCaptor.forClass(FileShare.class);
        verify(fileShareMapper).insert(captor.capture());
        assertNull(captor.getValue().getExpireTime());
    }

    @Test
    void saveShare_shouldUseProvidedCode_whenGiven() {
        FileShare share = new FileShare();
        share.setFileId("FILE001");
        share.setUserId("USER001");
        share.setValidType(ShareValidTypeEnums.FOREVER.getType());
        share.setCode("MYKEY");

        when(fileShareMapper.insert(any(FileShare.class))).thenReturn(1);

        fileShareService.saveShare(share);

        ArgumentCaptor<FileShare> captor = ArgumentCaptor.forClass(FileShare.class);
        verify(fileShareMapper).insert(captor.capture());
        assertEquals("MYKEY", captor.getValue().getCode());
    }

    @Test
    void saveShare_shouldThrowException_whenInvalidType() {
        FileShare share = new FileShare();
        share.setFileId("FILE001");
        share.setValidType(999);  // 非法类型

        assertThrows(BusinessException.class, () ->
            fileShareService.saveShare(share));
        verify(fileShareMapper, never()).insert(any());
    }

    // ==================== 取消分享 ====================

    @Test
    void deleteFileShareBatch_shouldDeleteAll() {
        String[] shareIds = {"S001", "S002", "S003"};
        when(fileShareMapper.deleteFileShareBatch(shareIds, "USER001")).thenReturn(3);

        fileShareService.deleteFileShareBatch(shareIds, "USER001");

        verify(fileShareMapper).deleteFileShareBatch(shareIds, "USER001");
    }

    @Test
    void deleteFileShareBatch_shouldThrowException_whenPartialDelete() {
        String[] shareIds = {"S001", "S002"};
        when(fileShareMapper.deleteFileShareBatch(shareIds, "USER001")).thenReturn(1);  // 只删了 1 个

        assertThrows(BusinessException.class, () ->
            fileShareService.deleteFileShareBatch(shareIds, "USER001"));
    }

    // ==================== 校验提取码 ====================

    @Test
    void checkShareCode_shouldReturnSessionDto_whenCodeCorrect() {
        FileShare share = new FileShare();
        share.setShareId("SHARE001");
        share.setUserId("USER001");
        share.setFileId("FILE001");
        share.setCode("ABCDE");
        share.setExpireTime(null);  // 永久有效
        when(fileShareMapper.selectByShareId("SHARE001")).thenReturn(share);
        doNothing().when(fileShareMapper).updateShareShowCount("SHARE001");

        SessionShareDto result = fileShareService.checkShareCode("SHARE001", "ABCDE");

        assertEquals("SHARE001", result.getShareId());
        assertEquals("USER001", result.getShareUserId());
        assertEquals("FILE001", result.getFileId());
        verify(fileShareMapper).updateShareShowCount("SHARE001");
    }

    @Test
    void checkShareCode_shouldThrowException_whenShareNotExist() {
        when(fileShareMapper.selectByShareId("NOTEXIST")).thenReturn(null);

        assertThrows(BusinessException.class, () ->
            fileShareService.checkShareCode("NOTEXIST", "ABCDE"));
    }

    @Test
    void checkShareCode_shouldThrowException_whenShareExpired() {
        FileShare share = new FileShare();
        share.setShareId("SHARE001");
        share.setCode("ABCDE");
        share.setExpireTime(new Date(System.currentTimeMillis() - 1000));  // 已过期
        when(fileShareMapper.selectByShareId("SHARE001")).thenReturn(share);

        assertThrows(BusinessException.class, () ->
            fileShareService.checkShareCode("SHARE001", "ABCDE"));
    }

    @Test
    void checkShareCode_shouldThrowException_whenCodeWrong() {
        FileShare share = new FileShare();
        share.setShareId("SHARE001");
        share.setCode("ABCDE");
        share.setExpireTime(null);
        when(fileShareMapper.selectByShareId("SHARE001")).thenReturn(share);

        assertThrows(BusinessException.class, () ->
            fileShareService.checkShareCode("SHARE001", "WRONG"));
    }
}
