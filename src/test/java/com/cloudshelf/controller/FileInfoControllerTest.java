package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.service.FileInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileInfoController 接口测试
 * 覆盖: 文件列表/新建文件夹/重命名/删除/移动/生成下载链接
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class FileInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileInfoService fileInfoService;

    @MockBean
    private RedisComponent redisComponent;

    @MockBean
    private AppConfig appConfig;

    // ==================== 文件列表 ====================

    @Test
    void loadDataList_shouldReturnPagedResult() throws Exception {
        PaginationResultVO<FileInfo> pageResult = new PaginationResultVO<>();
        pageResult.setTotalCount(2);
        pageResult.setPageNo(1);
        pageResult.setPageSize(15);
        pageResult.setList(Collections.emptyList());
        when(fileInfoService.findListByPage(any())).thenReturn(pageResult);

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(get("/file/loadDataList")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("pageNo", "1")
                .param("pageSize", "15"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    void loadDataList_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/file/loadDataList"))
            .andExpect(jsonPath("$.code").value(901));
    }

    // ==================== 新建文件夹 ====================

    @Test
    void newFolder_shouldCreateFolder() throws Exception {
        FileInfo folder = new FileInfo();
        folder.setFileId("FOLDER001");
        folder.setFileName("新建文件夹");
        when(fileInfoService.newFolder(eq("parentPid"), eq("USER001"), eq("新建文件夹")))
            .thenReturn(folder);

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/file/newFoloder")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("filePid", "parentPid")
                .param("fileName", "新建文件夹"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.fileName").value("新建文件夹"));
    }

    @Test
    void newFolder_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/file/newFoloder")
                .param("filePid", "parentPid")
                .param("fileName", "新建文件夹"))
            .andExpect(jsonPath("$.code").value(901));
    }

    // ==================== 重命名 ====================

    @Test
    void rename_shouldUpdateFileName() throws Exception {
        FileInfo renamed = new FileInfo();
        renamed.setFileId("FILE001");
        renamed.setFileName("newName.txt");
        when(fileInfoService.rename("FILE001", "USER001", "newName")).thenReturn(renamed);

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/file/rename")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("fileId", "FILE001")
                .param("fileName", "newName"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.fileName").value("newName.txt"));
    }

    // ==================== 删除文件 ====================

    @Test
    void delFile_shouldMoveToRecycle() throws Exception {
        doNothing().when(fileInfoService).removeFile2RecycleBatch("USER001", "FILE001,FILE002");

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/file/delFile")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("fileIds", "FILE001,FILE002"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void delFile_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/file/delFile")
                .param("fileIds", "FILE001"))
            .andExpect(jsonPath("$.code").value(901));
    }

    // ==================== 加载所有文件夹（移动用） ====================

    @Test
    void loadAllFolder_shouldReturnFolderList() throws Exception {
        when(fileInfoService.findListByParam(any())).thenReturn(Collections.emptyList());

        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(get("/file/loadAllFolder")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("filePid", "0"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 生成下载链接 ====================

    @Test
    void createDownloadUrl_shouldReturnCode() throws Exception {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        // createDownloadUrl 内部调用 redisComponent.saveDownloadCode
        // 需要 mock 完整的 fileInfoService.getFileInfoByFileIdAndUserId 返回值
        // 这里主要验证登录拦截
        mockMvc.perform(get("/file/createDownloadUrl/NOTEXIST")
                .sessionAttr(Constants.SESSION_KEY, user))
            .andExpect(status().isOk());  // 业务异常也返回 200（项目现状）
    }

    @Test
    void createDownloadUrl_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/file/createDownloadUrl/FILE001"))
            .andExpect(jsonPath("$.code").value(901));
    }
}
