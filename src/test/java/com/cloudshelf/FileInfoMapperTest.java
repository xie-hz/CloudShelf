package com.cloudshelf;

import com.cloudshelf.entity.enums.FileDelFlagEnums;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.entity.query.FileInfoQuery;
import com.cloudshelf.mappers.FileInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 最简单的 Mapper 测试 - 验证 H2 + MyBatis 环境是否正常
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
class FileInfoMapperTest {

    @Autowired
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    // ===== 1. 插入和查询 =====

    @Test
    void insert_shouldPersistFileInfo() {
        FileInfo fi = buildFileInfo("F001", "U001", "test.txt");
        assertEquals(1, fileInfoMapper.insert(fi));

        FileInfo saved = fileInfoMapper.selectByFileIdAndUserId("F001", "U001");
        assertNotNull(saved);
        assertEquals("test.txt", saved.getFileName());
    }

    // ===== 2. 按 userId 筛选 =====

    @Test
    void selectList_shouldFilterByUserId() {
        fileInfoMapper.insert(buildFileInfo("F001", "U_A", "a.txt"));
        fileInfoMapper.insert(buildFileInfo("F002", "U_B", "b.txt"));

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId("U_A");

        List<FileInfo> result = fileInfoMapper.selectList(query);
        assertEquals(1, result.size());
        assertEquals("a.txt", result.get(0).getFileName());
    }

    // ===== 3. count 统计 =====

    @Test
    void selectCount_shouldReturnCorrectTotal() {
        fileInfoMapper.insert(buildFileInfo("F001", "U_A", "a.txt"));
        fileInfoMapper.insert(buildFileInfo("F002", "U_A", "b.txt"));
        fileInfoMapper.insert(buildFileInfo("F003", "U_A", "c.txt"));

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId("U_A");

        assertEquals(3, fileInfoMapper.selectCount(query));
    }

    // ===== 4. 按 delFlag 筛选 =====

    @Test
    void selectList_shouldFilterByDelFlag() {
        FileInfo normal = buildFileInfo("F001", "U_A", "normal.txt");
        normal.setDelFlag(2);
        FileInfo recycled = buildFileInfo("F002", "U_A", "recycled.txt");
        recycled.setDelFlag(1);

        fileInfoMapper.insert(normal);
        fileInfoMapper.insert(recycled);

        FileInfoQuery query = new FileInfoQuery();
        query.setUserId("U_A");
        query.setDelFlag(2);

        List<FileInfo> result = fileInfoMapper.selectList(query);
        assertEquals(1, result.size());
        assertEquals("normal.txt", result.get(0).getFileName());
    }

    // ===== 5. 更新 =====

    @Test
    void updateByFileIdAndUserId_shouldModifyFileName() {
        fileInfoMapper.insert(buildFileInfo("F001", "U001", "old.txt"));

        FileInfo update = new FileInfo();
        update.setFileName("new.txt");
        fileInfoMapper.updateByFileIdAndUserId(update, "F001", "U001");

        FileInfo result = fileInfoMapper.selectByFileIdAndUserId("F001", "U001");
        assertEquals("new.txt", result.getFileName());
    }

    // ===== 6. 删除 =====

    @Test
    void deleteByFileIdAndUserId_shouldRemoveRecord() {
        fileInfoMapper.insert(buildFileInfo("F001", "U001", "test.txt"));
        fileInfoMapper.deleteByFileIdAndUserId("F001", "U001");

        FileInfo result = fileInfoMapper.selectByFileIdAndUserId("F001", "U001");
        assertNull(result);
    }

    // ===== 辅助方法 =====

    private FileInfo buildFileInfo(String fileId, String userId, String fileName) {
        FileInfo fi = new FileInfo();
        fi.setFileId(fileId);
        fi.setUserId(userId);
        fi.setFileName(fileName);
        fi.setCreateTime(new Date());
        fi.setLastUpdateTime(new Date());
        fi.setDelFlag(2);
        fi.setStatus(2);
        fi.setFolderType(0);
        fi.setFileCategory(5);
        fi.setFileType(10);
        return fi;
    }
}
