package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionShareDto;
import com.cloudshelf.entity.po.FileInfo;
import com.cloudshelf.entity.po.FileShare;
import com.cloudshelf.entity.po.UserInfo;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.service.FileInfoService;
import com.cloudshelf.service.FileShareService;
import com.cloudshelf.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WebShareController 接口测试（公开分享 - 无需登录）
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WebShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileShareService fileShareService;
    @MockBean private FileInfoService fileInfoService;
    @MockBean private UserInfoService userInfoService;
    @MockBean private RedisComponent redisComponent;
    @MockBean private AppConfig appConfig;

    // ==================== 获取分享信息 ====================

    @Test
    void getShareInfo_shouldReturnShareInfo() throws Exception {
        FileShare share = new FileShare();
        share.setShareId("SHARE001");
        share.setFileId("FILE001");
        share.setUserId("USER001");
        share.setExpireTime(null);
        when(fileShareService.getFileShareByShareId("SHARE001")).thenReturn(share);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName("测试文件.txt");
        fileInfo.setDelFlag(2);
        when(fileInfoService.getFileInfoByFileIdAndUserId("FILE001", "USER001"))
            .thenReturn(fileInfo);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("USER001");
        userInfo.setNickName("分享者");
        when(userInfoService.getUserInfoByUserId("USER001")).thenReturn(userInfo);

        mockMvc.perform(get("/showShare/getShareInfo")
                .param("shareId", "SHARE001"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.nickName").value("分享者"));
    }

    @Test
    void getShareInfo_shouldReturn902_whenShareExpired() throws Exception {
        when(fileShareService.getFileShareByShareId("EXPIRED")).thenReturn(null);

        mockMvc.perform(get("/showShare/getShareInfo")
                .param("shareId", "EXPIRED"))
            .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== 校验提取码 ====================

    @Test
    void checkShareCode_shouldSucceed_whenCodeCorrect() throws Exception {
        SessionShareDto sessionDto = new SessionShareDto();
        sessionDto.setShareId("SHARE001");
        sessionDto.setShareUserId("USER001");
        sessionDto.setFileId("FILE001");
        when(fileShareService.checkShareCode("SHARE001", "ABCDE")).thenReturn(sessionDto);

        mockMvc.perform(post("/showShare/checkShareCode")
                .param("shareId", "SHARE001")
                .param("code", "ABCDE"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void checkShareCode_shouldReturnError_whenCodeWrong() throws Exception {
        when(fileShareService.checkShareCode("SHARE001", "WRONG"))
            .thenThrow(new com.cloudshelf.exception.BusinessException("提取码错误"));

        mockMvc.perform(post("/showShare/checkShareCode")
                .param("shareId", "SHARE001")
                .param("code", "WRONG"))
            .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== 加载分享文件列表 ====================

    @Test
    void loadFileList_shouldReturn903_whenNotChecked() throws Exception {
        // 未先校验提取码，session 里没有 SessionShareDto
        mockMvc.perform(get("/showShare/loadFileList")
                .param("shareId", "SHARE001"))
            .andExpect(jsonPath("$.code").value(903));
    }

    @Test
    void loadFileList_shouldReturnList_whenChecked() throws Exception {
        SessionShareDto sessionDto = new SessionShareDto();
        sessionDto.setShareId("SHARE001");
        sessionDto.setShareUserId("USER001");
        sessionDto.setFileId("FILE001");
        sessionDto.setExpireTime(null);

        when(fileInfoService.findListByPage(any())).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(get("/showShare/loadFileList")
                .param("shareId", "SHARE001")
                .sessionAttr(Constants.SESSION_SHARE_KEY + "SHARE001", sessionDto))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 保存分享到我的网盘 ====================

    @Test
    void saveShare_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/showShare/saveShare")
                .param("shareId", "SHARE001")
                .param("shareFileIds", "FILE001")
                .param("myFolderId", "0"))
            .andExpect(jsonPath("$.code").value(901));
    }
}
