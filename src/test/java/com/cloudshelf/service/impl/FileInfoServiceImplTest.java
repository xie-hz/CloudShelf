package com.cloudshelf.service.impl;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.dto.UploadResultDto;
import com.cloudshelf.entity.dto.UserSpaceDto;
import com.cloudshelf.entity.enums.FileDelFlagEnums;
import com.cloudshelf.entity.enums.FileFolderTypeEnums;
import com.cloudshelf.entity.enums.FileStatusEnums;
import com.cloudshelf.entity.enums.UploadStatusEnums;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.entity.po.UserInfo;
import com.cloudshelf.entity.query.FileInfoQuery;
import com.cloudshelf.entity.query.UserInfoQuery;
import com.cloudshelf.exception.BusinessException;
import com.cloudshelf.mappers.FileInfoMapper;
import com.cloudshelf.mappers.UserInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileInfoServiceImplTest {

    @Mock private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
    @Mock private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Mock private RedisComponent redisComponent;
    @Mock private AppConfig appConfig;

    @InjectMocks
    private FileInfoServiceImpl fileInfoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileInfoService, "fileInfoService", fileInfoService);
    }

    // ==================== 新建文件夹 ====================

    @Test
    void newFolder_shouldCreateFolder_whenNameAvailable() {
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        FileInfo result = fileInfoService.newFolder("parentPid", "USER001", "新建文件夹");

        assertNotNull(result.getFileId());
        assertEquals(10, result.getFileId().length());
        assertEquals("新建文件夹", result.getFileName());
        assertEquals("parentPid", result.getFilePid());
        assertEquals(FileFolderTypeEnums.FOLDER.getType(), result.getFolderType());
    }

    @Test
    void newFolder_shouldThrowException_whenNameExists() {
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(1);
        assertThrows(BusinessException.class, () ->
            fileInfoService.newFolder("parentPid", "USER001", "重复文件夹"));
    }

    // ==================== 重命名 ====================

    @Test
    void rename_shouldKeepSuffix_whenRenamingFile() {
        FileInfo existing = new FileInfo();
        existing.setFileId("FILE001"); existing.setFileName("old.txt");
        existing.setFilePid("parent"); existing.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectByFileIdAndUserId("FILE001", "USER001")).thenReturn(existing);
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0);
        when(fileInfoMapper.updateByFileIdAndUserId(any(FileInfo.class), eq("FILE001"), eq("USER001"))).thenReturn(1);

        FileInfo result = fileInfoService.rename("FILE001", "USER001", "new");
        assertEquals("new.txt", result.getFileName());
    }

    @Test
    void rename_shouldThrowException_whenFileNotExists() {
        when(fileInfoMapper.selectByFileIdAndUserId("NOTEXIST", "USER001")).thenReturn(null);
        assertThrows(BusinessException.class, () ->
            fileInfoService.rename("NOTEXIST", "USER001", "newName"));
    }

    @Test
    void rename_shouldAutoRename_whenNameConflict() {
        FileInfo existing = new FileInfo();
        existing.setFileId("FILE001"); existing.setFileName("old.txt");
        existing.setFilePid("parent"); existing.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectByFileIdAndUserId("FILE001", "USER001")).thenReturn(existing);
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0).thenReturn(1);
        when(fileInfoMapper.updateByFileIdAndUserId(any(), eq("FILE001"), eq("USER001"))).thenReturn(1);

        FileInfo result = fileInfoService.rename("FILE001", "USER001", "old");
        assertNotNull(result.getFileName());
    }

    // ==================== 移动文件 ====================

    @Test
    void changeFileFolder_shouldThrowException_whenMoveToSelf() {
        assertThrows(BusinessException.class, () ->
            fileInfoService.changeFileFolder("FILE001", "FILE001", "USER001"));
    }

    @Test
    void changeFileFolder_shouldUpdateFilePid_whenTargetExists() {
        FileInfo targetFolder = new FileInfo();
        targetFolder.setFileId("FOLDER001"); targetFolder.setDelFlag(FileDelFlagEnums.USING.getFlag());
        when(fileInfoMapper.selectByFileIdAndUserId("FOLDER001", "USER001")).thenReturn(targetFolder);

        FileInfo fileToMove = new FileInfo();
        fileToMove.setFileId("FILE001"); fileToMove.setFileName("test.txt");
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(fileToMove));

        fileInfoService.changeFileFolder("FILE001", "FOLDER001", "USER001");
        verify(fileInfoMapper).updateByFileIdAndUserId(any(FileInfo.class), eq("FILE001"), eq("USER001"));
    }

    // ==================== 删除到回收站 ====================

    @Test
    void removeFile2Recycle_shouldMarkFileAsRecycled() {
        FileInfo file = new FileInfo();
        file.setFileId("FILE001"); file.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(file))
            .thenReturn(Collections.<FileInfo>emptyList());
        doNothing().when(fileInfoMapper).updateFileDelFlagBatch(
            any(FileInfo.class), anyString(), any(), any(), anyInt());

        fileInfoService.removeFile2RecycleBatch("USER001", "FILE001");
        verify(fileInfoMapper, atLeastOnce()).updateFileDelFlagBatch(
            any(FileInfo.class), eq("USER001"), any(), any(), anyInt());
    }

    @Test
    void removeFile2Recycle_shouldAlsoRecycleSubFiles_whenFolderDeleted() {
        FileInfo folder = new FileInfo();
        folder.setFileId("FOLDER001"); folder.setFolderType(FileFolderTypeEnums.FOLDER.getType());

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(folder))
            .thenReturn(Collections.<FileInfo>emptyList());
        doNothing().when(fileInfoMapper).updateFileDelFlagBatch(
            any(FileInfo.class), anyString(), any(), any(), anyInt());

        fileInfoService.removeFile2RecycleBatch("USER001", "FOLDER001");
        verify(fileInfoMapper, atLeastOnce()).updateFileDelFlagBatch(
            any(FileInfo.class), eq("USER001"), any(), any(), anyInt());
    }

    // ==================== 从回收站恢复 ====================

    @Test
    void recoverFile_shouldRestoreFileToRoot() {
        FileInfo recycledFile = new FileInfo();
        recycledFile.setFileId("FILE001"); recycledFile.setFileName("test.txt");
        recycledFile.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(recycledFile))
            .thenReturn(Collections.<FileInfo>emptyList());
        doNothing().when(fileInfoMapper).updateFileDelFlagBatch(
            any(FileInfo.class), anyString(), any(), any(), anyInt());

        fileInfoService.recoverFileBatch("USER001", "FILE001");
        verify(fileInfoMapper, atLeastOnce()).updateFileDelFlagBatch(
            any(FileInfo.class), eq("USER001"), any(), any(), anyInt());
    }

    // ==================== 彻底删除 ====================

    @Test
    void delFile_shouldPermanentlyDelete() {
        FileInfo recycledFile = new FileInfo();
        recycledFile.setFileId("FILE001"); recycledFile.setFolderType(FileFolderTypeEnums.FILE.getType());

        UserSpaceDto spaceDto = new UserSpaceDto(); spaceDto.setUseSpace(0L);
        when(redisComponent.getUserSpaceUse("USER001")).thenReturn(spaceDto);
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(recycledFile));
        doNothing().when(fileInfoMapper).delFileBatch(anyString(), any(), any(), any());
        when(fileInfoMapper.selectUseSpace("USER001")).thenReturn(0L);
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq("USER001"))).thenReturn(1);

        fileInfoService.delFileBatch("USER001", "FILE001", false);

        verify(fileInfoMapper).delFileBatch(eq("USER001"), isNull(), any(), eq(FileDelFlagEnums.RECYCLE.getFlag()));
        verify(userInfoMapper).updateByUserId(any(UserInfo.class), eq("USER001"));
    }

    // ==================== 保存分享到我的网盘 ====================

    @Test
    void saveShare_shouldCopyFiles() {
        FileInfo shareFile = new FileInfo();
        shareFile.setFileId("SRC001"); shareFile.setFileName("shared.txt");
        shareFile.setFilePid("0"); shareFile.setFolderType(FileFolderTypeEnums.FILE.getType());
        shareFile.setFileSize(1024L);

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(shareFile));
        when(fileInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(fileInfoMapper.selectUseSpace("USER002")).thenReturn(1024L);

        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER002"); dbUser.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(userInfoMapper.selectByUserId("USER002")).thenReturn(dbUser);
        when(userInfoMapper.updateByUserId(any(), eq("USER002"))).thenReturn(1);

        UserSpaceDto spaceDto = new UserSpaceDto(); spaceDto.setUseSpace(0L);
        when(redisComponent.getUserSpaceUse("USER002")).thenReturn(spaceDto);

        fileInfoService.saveShare("ROOT001", "SRC001", "MYFOLDER", "USER001", "USER002");
        verify(fileInfoMapper).insertBatch(anyList());
        verify(userInfoMapper).updateByUserId(any(UserInfo.class), eq("USER002"));
    }

    @Test
    void saveShare_shouldThrowException_whenSpaceExceeded() {
        FileInfo shareFile = new FileInfo();
        shareFile.setFileId("SRC001"); shareFile.setFileName("bigfile.txt");
        shareFile.setFolderType(FileFolderTypeEnums.FILE.getType());
        shareFile.setFileSize(5L * 1024 * 1024 * 1024);

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(shareFile));
        when(fileInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(fileInfoMapper.selectUseSpace("USER002")).thenReturn(5L * 1024 * 1024 * 1024);

        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER002"); dbUser.setTotalSpace(1L * 1024 * 1024 * 1024);
        when(userInfoMapper.selectByUserId("USER002")).thenReturn(dbUser);

        assertThrows(BusinessException.class, () ->
            fileInfoService.saveShare("ROOT001", "SRC001", "MYFOLDER", "USER001", "USER002"));
    }

    // ==================== checkRootFilePid ====================

    @Test
    void checkRootFilePid_shouldThrowException_whenFileIdEmpty() {
        assertThrows(BusinessException.class, () ->
            fileInfoService.checkRootFilePid("ROOT", "USER001", ""));
    }

    @Test
    void checkRootFilePid_shouldReturn_whenSameAsRoot() {
        assertDoesNotThrow(() ->
            fileInfoService.checkRootFilePid("ROOT", "USER001", "ROOT"));
    }

    // ==================== uploadFile - 秒传 ====================

    @Test
    void uploadFile_shouldSecondsUpload_whenMd5Hit() {
        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(1024L * 1024L); spaceDto.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(redisComponent.getUserSpaceUse("USER001")).thenReturn(spaceDto);
        doReturn(1).when(userInfoMapper).updateUserSpace(eq("USER001"), any(), isNull());

        FileInfo existing = new FileInfo();
        existing.setFileId("OLD001"); existing.setUserId("OTHER_USER");
        existing.setFileName("movie.mp4"); existing.setFilePath("202605/OTHER_USEROLD001.mp4");
        existing.setFileSize(50L * 1024 * 1024); existing.setFileMd5("abc123");
        existing.setStatus(FileStatusEnums.USING.getStatus());
        existing.setDelFlag(FileDelFlagEnums.USING.getFlag());
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(existing));
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0);
        when(fileInfoMapper.insert(any(FileInfo.class))).thenReturn(1);

        SessionWebUserDto user = new SessionWebUserDto(); user.setUserId("USER001");
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(50L * 1024 * 1024);

        UploadResultDto result = fileInfoService.uploadFile(
            user, null, file, "movie.mp4", "ROOT", "abc123", 0, 3);

        assertEquals(UploadStatusEnums.UPLOAD_SECONDS.getCode(), result.getStatus());
        assertNotNull(result.getFileId());
        assertEquals(10, result.getFileId().length());

        ArgumentCaptor<FileInfo> captor = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(captor.capture());
        FileInfo saved = captor.getValue();
        assertEquals("USER001", saved.getUserId());
        assertEquals("ROOT", saved.getFilePid());
        assertEquals(FileStatusEnums.USING.getStatus(), saved.getStatus());
    }

    @Test
    void uploadFile_shouldThrowException_whenMd5HitButSpaceFull() {
        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(10L * 1024 * 1024 * 1024);
        spaceDto.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(redisComponent.getUserSpaceUse("USER001")).thenReturn(spaceDto);

        FileInfo existing = new FileInfo();
        existing.setFileSize(1024L * 1024L);
        existing.setStatus(FileStatusEnums.USING.getStatus());
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(existing));

        SessionWebUserDto user = new SessionWebUserDto(); user.setUserId("USER001");
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1L);

        assertThrows(BusinessException.class, () ->
            fileInfoService.uploadFile(user, null, file, "test.txt", "pid", "abc123", 0, 1));
    }
}
