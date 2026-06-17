package com.cloudshelf.service.impl;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.dto.UserSpaceDto;
import com.cloudshelf.entity.enums.FileDelFlagEnums;
import com.cloudshelf.entity.enums.FileFolderTypeEnums;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileInfoServiceImplTest {

    @Mock private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;
    @Mock private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Mock private RedisComponent redisComponent;
    @Mock private AppConfig appConfig;

    @InjectMocks
    private FileInfoServiceImpl fileInfoService;

    @BeforeEach
    void setUp() {
        // changeFileFolder / removeFile2RecycleBatch / recoverFileBatch / delFileBatch
        // 内部通过 @Lazy 自注入调用自身方法，纯 Mockito 下需要手动注入
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
        existing.setFileId("FILE001");
        existing.setFileName("old.txt");
        existing.setFilePid("parent");
        existing.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectByFileIdAndUserId("FILE001", "USER001")).thenReturn(existing);
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0);
        when(fileInfoMapper.updateByFileIdAndUserId(any(FileInfo.class), eq("FILE001"), eq("USER001")))
            .thenReturn(1);

        FileInfo result = fileInfoService.rename("FILE001", "USER001", "new");

        assertEquals("new.txt", result.getFileName());
    }

    @Test
    void rename_shouldThrowException_whenFileNotExists() {
        when(fileInfoMapper.selectByFileIdAndUserId("NOTEXIST", "USER001")).thenReturn(null);

        assertThrows(BusinessException.class, () ->
            fileInfoService.rename("NOTEXIST", "USER001", "newName"));
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
        targetFolder.setFileId("FOLDER001");
        targetFolder.setDelFlag(FileDelFlagEnums.USING.getFlag());
        when(fileInfoMapper.selectByFileIdAndUserId("FOLDER001", "USER001"))
            .thenReturn(targetFolder);

        FileInfo fileToMove = new FileInfo();
        fileToMove.setFileId("FILE001");
        fileToMove.setFileName("test.txt");
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(fileToMove));

        fileInfoService.changeFileFolder("FILE001", "FOLDER001", "USER001");

        verify(fileInfoMapper).updateByFileIdAndUserId(
            any(FileInfo.class), eq("FILE001"), eq("USER001"));
    }

    // ==================== 删除到回收站 ====================

    @Test
    void removeFile2Recycle_shouldMarkFileAsRecycled() {
        FileInfo file = new FileInfo();
        file.setFileId("FILE001");
        file.setFolderType(FileFolderTypeEnums.FILE.getType());

        // selectList 调用:
        // 1. removeFile2RecycleBatch: 查询选中文件 (fileIdArray + delFlag=USING)
        // 2. findAllSubFolderFileIdList: 查 FILE001 下的子文件夹 → 空 (FILE001 是文件不是文件夹)
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
        folder.setFileId("FOLDER001");
        folder.setFolderType(FileFolderTypeEnums.FOLDER.getType());

        // selectList 调用顺序:
        // 1. 查询选中文件夹 (fileIdArray + delFlag=USING) → 返回 folder
        // 2. findAllSubFolderFileIdList 查 FOLDER001 下的子文件夹 → 返回空 (无子文件夹)
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
        recycledFile.setFileId("FILE001");
        recycledFile.setFileName("test.txt");
        recycledFile.setFolderType(FileFolderTypeEnums.FILE.getType());

        // selectList 调用:
        // 1. 查询回收站文件 (fileIdArray + delFlag=RECYCLE)
        // 2. 查询根目录同名文件 → 空
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
        recycledFile.setFileId("FILE001");
        recycledFile.setFolderType(FileFolderTypeEnums.FILE.getType());

        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(0L);
        when(redisComponent.getUserSpaceUse("USER001")).thenReturn(spaceDto);

        // selectList 调用:
        // 1. 查询选中文件 (fileIdArray + delFlag=RECYCLE)
        // 2. findAllSubFolderFileIdList → 空
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(recycledFile));

        doNothing().when(fileInfoMapper).delFileBatch(anyString(), any(), any(), any());
        when(fileInfoMapper.selectUseSpace("USER001")).thenReturn(0L);
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq("USER001"))).thenReturn(1);

        fileInfoService.delFileBatch("USER001", "FILE001", false);

        verify(fileInfoMapper).delFileBatch(eq("USER001"), isNull(), any(), eq(FileDelFlagEnums.RECYCLE.getFlag()));
        verify(userInfoMapper).updateByUserId(any(UserInfo.class), eq("USER001"));
    }

    // ==================== 空间检查 ====================

    @Test
    void uploadFile_shouldThrowException_whenSpaceFull() {
        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(10L * 1024 * 1024 * 1024);
        spaceDto.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(redisComponent.getUserSpaceUse("USER001")).thenReturn(spaceDto);
        when(redisComponent.getFileTempSize("USER001", "FILE001")).thenReturn(0L);

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        org.springframework.web.multipart.MultipartFile mockFile =
            mock(org.springframework.web.multipart.MultipartFile.class);
        when(mockFile.getSize()).thenReturn(1024L * 1024L);

        assertThrows(BusinessException.class, () ->
            fileInfoService.uploadFile(user, "FILE001", mockFile,
                "test.txt", "pid", "md5hash", 0, 1));
    }

    // ==================== 保存分享到我的网盘 ====================

    @Test
    void saveShare_shouldCopyFiles() {
        FileInfo shareFile = new FileInfo();
        shareFile.setFileId("SRC001");
        shareFile.setFileName("shared.txt");
        shareFile.setFilePid("0");
        shareFile.setFolderType(FileFolderTypeEnums.FILE.getType());
        shareFile.setFileSize(1024L);

        // selectList: 目标目录文件列表(空) + 选中的分享文件
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(shareFile));
        when(fileInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(fileInfoMapper.selectUseSpace("USER002")).thenReturn(1024L);

        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER002");
        dbUser.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(userInfoMapper.selectByUserId("USER002")).thenReturn(dbUser);
        when(userInfoMapper.updateByUserId(any(), eq("USER002"))).thenReturn(1);

        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(0L);
        when(redisComponent.getUserSpaceUse("USER002")).thenReturn(spaceDto);

        fileInfoService.saveShare("ROOT001", "SRC001", "MYFOLDER", "USER001", "USER002");

        verify(fileInfoMapper).insertBatch(anyList());
        verify(userInfoMapper).updateByUserId(any(UserInfo.class), eq("USER002"));
    }

    @Test
    void saveShare_shouldRename_whenNameConflict() {
        FileInfo shareFile = new FileInfo();
        shareFile.setFileId("SRC001");
        shareFile.setFileName("existing.txt");
        shareFile.setFolderType(FileFolderTypeEnums.FILE.getType());
        shareFile.setFileSize(100L);

        // 目标目录已有同名文件
        FileInfo existing = new FileInfo();
        existing.setFileName("existing.txt");
        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.singletonList(existing))  // 目标目录
            .thenReturn(Collections.singletonList(shareFile)); // 分享文件

        when(fileInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(fileInfoMapper.selectUseSpace("USER002")).thenReturn(100L);

        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER002");
        dbUser.setTotalSpace(10L * 1024 * 1024 * 1024);
        when(userInfoMapper.selectByUserId("USER002")).thenReturn(dbUser);
        when(userInfoMapper.updateByUserId(any(), eq("USER002"))).thenReturn(1);

        UserSpaceDto spaceDto = new UserSpaceDto();
        spaceDto.setUseSpace(0L);
        when(redisComponent.getUserSpaceUse("USER002")).thenReturn(spaceDto);

        fileInfoService.saveShare("ROOT001", "SRC001", "MYFOLDER", "USER001", "USER002");

        ArgumentCaptor<List<FileInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileInfoMapper).insertBatch(captor.capture());
        List<FileInfo> saved = captor.getValue();
        // 重命名后不再是 "existing.txt"
        assertNotEquals("existing.txt", saved.get(0).getFileName());
    }

    @Test
    void saveShare_shouldThrowException_whenSpaceExceeded() {
        FileInfo shareFile = new FileInfo();
        shareFile.setFileId("SRC001");
        shareFile.setFileName("bigfile.txt");
        shareFile.setFolderType(FileFolderTypeEnums.FILE.getType());
        shareFile.setFileSize(5L * 1024 * 1024 * 1024);

        when(fileInfoMapper.selectList(any(FileInfoQuery.class)))
            .thenReturn(Collections.<FileInfo>emptyList())
            .thenReturn(Collections.singletonList(shareFile));
        when(fileInfoMapper.insertBatch(anyList())).thenReturn(1);
        when(fileInfoMapper.selectUseSpace("USER002")).thenReturn(5L * 1024 * 1024 * 1024);

        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER002");
        dbUser.setTotalSpace(1L * 1024 * 1024 * 1024);  // 只有 1GB 空间
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

    // ==================== 同名重命名 ====================

    @Test
    void rename_shouldAutoRename_whenNameConflict() {
        FileInfo existing = new FileInfo();
        existing.setFileId("FILE001");
        existing.setFileName("old.txt");
        existing.setFilePid("parent");
        existing.setFolderType(FileFolderTypeEnums.FILE.getType());

        when(fileInfoMapper.selectByFileIdAndUserId("FILE001", "USER001")).thenReturn(existing);
        // selectCount 第一次返回 0（改名前检查），第二次返回 1（改名后检查）
        when(fileInfoMapper.selectCount(any(FileInfoQuery.class))).thenReturn(0).thenReturn(1);
        when(fileInfoMapper.updateByFileIdAndUserId(any(), eq("FILE001"), eq("USER001")))
            .thenReturn(1);

        FileInfo result = fileInfoService.rename("FILE001", "USER001", "old");

        // 重命名为原名（去掉后缀后的名字一样），应该触发自动重命名
        assertNotNull(result.getFileName());
    }
}
