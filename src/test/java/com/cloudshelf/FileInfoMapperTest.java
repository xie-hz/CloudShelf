package com.cloudshelf;

import com.cloudshelf.entity.enums.FileDelFlagEnums;
import com.cloudshelf.entity.enums.FileFolderTypeEnums;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.entity.query.FileInfoQuery;
import com.cloudshelf.mappers.FileInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileInfoMapper H2 集成测试
 * 覆盖: CRUD / 多条件筛选 / 按 filePid 查子文件 / 批量更新 delFlag
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class FileInfoMapperTest {

    @Autowired
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    // ==================== 基础 CRUD ====================

    @Test
    void insert_shouldPersistFileInfo() {
        assertEquals(1, fileInfoMapper.insert(buildFile("F001", "U001", "test.txt")));
        FileInfo saved = fileInfoMapper.selectByFileIdAndUserId("F001", "U001");
        assertNotNull(saved);
        assertEquals("test.txt", saved.getFileName());
    }

    @Test
    void selectList_shouldFilterByUserId() {
        fileInfoMapper.insert(buildFile("F001", "U_A", "a.txt"));
        fileInfoMapper.insert(buildFile("F002", "U_B", "b.txt"));
        FileInfoQuery q = new FileInfoQuery(); q.setUserId("U_A");
        assertEquals(1, fileInfoMapper.selectList(q).size());
    }

    @Test
    void selectCount_shouldReturnCorrectTotal() {
        fileInfoMapper.insert(buildFile("F001", "U_A", "a.txt"));
        fileInfoMapper.insert(buildFile("F002", "U_A", "b.txt"));
        FileInfoQuery q = new FileInfoQuery(); q.setUserId("U_A");
        assertEquals(2, fileInfoMapper.selectCount(q));
    }

    @Test
    void selectList_shouldFilterByDelFlag() {
        FileInfo normal = buildFile("F001", "U_A", "n.txt");
        normal.setDelFlag(2);
        FileInfo recycled = buildFile("F002", "U_A", "r.txt");
        recycled.setDelFlag(1);
        fileInfoMapper.insert(normal);
        fileInfoMapper.insert(recycled);
        FileInfoQuery q = new FileInfoQuery(); q.setUserId("U_A"); q.setDelFlag(2);
        assertEquals(1, fileInfoMapper.selectList(q).size());
    }

    @Test
    void updateByFileIdAndUserId_shouldModifyFileName() {
        fileInfoMapper.insert(buildFile("F001", "U001", "old.txt"));
        FileInfo update = new FileInfo(); update.setFileName("new.txt");
        fileInfoMapper.updateByFileIdAndUserId(update, "F001", "U001");
        assertEquals("new.txt", fileInfoMapper.selectByFileIdAndUserId("F001", "U001").getFileName());
    }

    @Test
    void deleteByFileIdAndUserId_shouldRemoveRecord() {
        fileInfoMapper.insert(buildFile("F001", "U001", "test.txt"));
        fileInfoMapper.deleteByFileIdAndUserId("F001", "U001");
        assertNull(fileInfoMapper.selectByFileIdAndUserId("F001", "U001"));
    }

    // ==================== 按 filePid 查子文件 ====================

    @Test
    void selectList_shouldFilterByFilePid() {
        fileInfoMapper.insert(buildFolder("FOLDER_A", "U001", "root", "0"));
        fileInfoMapper.insert(buildFile("F001", "U001", "child1.txt", "FOLDER_A"));
        fileInfoMapper.insert(buildFile("F002", "U001", "child2.txt", "FOLDER_A"));

        FileInfoQuery q = new FileInfoQuery(); q.setUserId("U001"); q.setFilePid("FOLDER_A");
        List<FileInfo> result = fileInfoMapper.selectList(q);
        assertEquals(2, result.size());
    }

    // ==================== 按 fileCategory 分类筛选 ====================

    @Test
    void selectList_shouldFilterByFileCategory() {
        FileInfo video = buildFile("F001", "U001", "movie.mp4");
        video.setFileCategory(1);  // 视频
        FileInfo image = buildFile("F002", "U001", "photo.jpg");
        image.setFileCategory(3);  // 图片
        fileInfoMapper.insert(video);
        fileInfoMapper.insert(image);

        FileInfoQuery q = new FileInfoQuery(); q.setUserId("U001"); q.setFileCategory(1);
        List<FileInfo> result = fileInfoMapper.selectList(q);
        assertEquals(1, result.size());
        assertEquals("movie.mp4", result.get(0).getFileName());
    }

    // ==================== 按 fileIdArray 批量查询 ====================

    @Test
    void selectList_shouldFilterByFileIdArray() {
        fileInfoMapper.insert(buildFile("F001", "U001", "a.txt"));
        fileInfoMapper.insert(buildFile("F002", "U001", "b.txt"));
        fileInfoMapper.insert(buildFile("F003", "U001", "c.txt"));

        FileInfoQuery q = new FileInfoQuery();
        q.setUserId("U001");
        q.setFileIdArray(new String[]{"F001", "F003"});

        List<FileInfo> result = fileInfoMapper.selectList(q);
        assertEquals(2, result.size());
    }

    // ==================== updateFileDelFlagBatch ====================

    @Test
    void updateFileDelFlagBatch_shouldUpdateDelFlag() {
        fileInfoMapper.insert(buildFile("F001", "U001", "a.txt"));

        FileInfo update = new FileInfo();
        update.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        update.setRecoveryTime(new Date());
        fileInfoMapper.updateFileDelFlagBatch(
            update, "U001", null, Collections.singletonList("F001"), FileDelFlagEnums.USING.getFlag());

        // 查回收站状态
        FileInfoQuery q = new FileInfoQuery();
        q.setUserId("U001"); q.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        assertEquals(1, fileInfoMapper.selectCount(q));
    }

    // ==================== selectUseSpace ====================

    @Test
    void selectUseSpace_shouldSumFileSize() {
        FileInfo f1 = buildFile("F001", "U001", "a.txt");
        f1.setFileSize(1024L);
        FileInfo f2 = buildFile("F002", "U001", "b.txt");
        f2.setFileSize(2048L);
        fileInfoMapper.insert(f1);
        fileInfoMapper.insert(f2);

        Long total = fileInfoMapper.selectUseSpace("U001");
        assertEquals(Long.valueOf(3072L), total);
    }

    // ==================== insertBatch 批量插入 ====================

    @Test
    void insertBatch_shouldPersistMultiple() {
        List<FileInfo> list = Arrays.asList(
            buildFile("F001", "U001", "a.txt"),
            buildFile("F002", "U001", "b.txt")
        );
        assertEquals(2, fileInfoMapper.insertBatch(list));
        assertEquals(2, fileInfoMapper.selectCount(new FileInfoQuery() {{ setUserId("U001"); }}));
    }

    // ==================== 辅助方法 ====================

    private FileInfo buildFile(String fileId, String userId, String fileName) {
        return buildFile(fileId, userId, fileName, "0");
    }

    private FileInfo buildFile(String fileId, String userId, String fileName, String filePid) {
        FileInfo fi = new FileInfo();
        fi.setFileId(fileId); fi.setUserId(userId); fi.setFileName(fileName);
        fi.setFilePid(filePid);
        fi.setCreateTime(new Date()); fi.setLastUpdateTime(new Date());
        fi.setDelFlag(2); fi.setStatus(2); fi.setFolderType(0);
        fi.setFileCategory(5); fi.setFileType(10);
        return fi;
    }

    private FileInfo buildFolder(String fileId, String userId, String folderName, String filePid) {
        FileInfo fi = buildFile(fileId, userId, folderName, filePid);
        fi.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        return fi;
    }
}
