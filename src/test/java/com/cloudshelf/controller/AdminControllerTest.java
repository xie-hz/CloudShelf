package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.dto.SysSettingsDto;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.service.FileInfoService;
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
 * AdminController 接口测试
 * 重点: 管理员权限校验 / 系统设置 / 用户管理
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RedisComponent redisComponent;
    @MockBean private UserInfoService userInfoService;
    @MockBean private FileInfoService fileInfoService;
    @MockBean private AppConfig appConfig;

    private SessionWebUserDto adminUser() {
        SessionWebUserDto admin = new SessionWebUserDto();
        admin.setUserId("ADMIN001");
        admin.setNickName("管理员");
        admin.setAdmin(true);
        return admin;
    }

    private SessionWebUserDto normalUser() {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");
        user.setAdmin(false);
        return user;
    }

    // ==================== 系统设置 ====================

    @Test
    void getSysSettings_shouldReturn404_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/getSysSettings")
                .sessionAttr(Constants.SESSION_KEY, normalUser()))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void getSysSettings_shouldReturnSettings_whenAdmin() throws Exception {
        when(redisComponent.getSysSettingsDto()).thenReturn(new SysSettingsDto() {{
            setUserInitUseSpace(1024);
            setRegisterEmailTitle("验证码");
            setRegisterEmailContent("您的验证码：%s");
        }});

        mockMvc.perform(get("/admin/getSysSettings")
                .sessionAttr(Constants.SESSION_KEY, adminUser()))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getSysSettings_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/getSysSettings"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void saveSysSettings_shouldReturn404_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/admin/saveSysSettings")
                .sessionAttr(Constants.SESSION_KEY, normalUser())
                .param("registerEmailTitle", "验证码")
                .param("registerEmailContent", "您的验证码：%s")
                .param("userInitUseSpace", "1024"))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void saveSysSettings_shouldSave_whenAdmin() throws Exception {
        doNothing().when(redisComponent).saveSysSettingsDto(any());

        mockMvc.perform(post("/admin/saveSysSettings")
                .sessionAttr(Constants.SESSION_KEY, adminUser())
                .param("registerEmailTitle", "验证码")
                .param("registerEmailContent", "您的验证码：%s")
                .param("userInitUseSpace", "1024"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 用户管理 ====================

    @Test
    void loadUserList_shouldReturn404_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/admin/loadUserList")
                .sessionAttr(Constants.SESSION_KEY, normalUser()))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void loadUserList_shouldReturnList_whenAdmin() throws Exception {
        when(userInfoService.findListByPage(any())).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(get("/admin/loadUserList")
                .sessionAttr(Constants.SESSION_KEY, adminUser()))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void updateUserStatus_shouldReturn404_whenNotAdmin() throws Exception {
        mockMvc.perform(post("/admin/updateUserStatus")
                .sessionAttr(Constants.SESSION_KEY, normalUser())
                .param("userId", "USER001")
                .param("status", "0"))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateUserStatus_shouldSucceed_whenAdmin() throws Exception {
        doNothing().when(userInfoService).updateUserStatus("USER001", 0);

        mockMvc.perform(post("/admin/updateUserStatus")
                .sessionAttr(Constants.SESSION_KEY, adminUser())
                .param("userId", "USER001")
                .param("status", "0"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void updateUserSpace_shouldSucceed_whenAdmin() throws Exception {
        doNothing().when(userInfoService).changeUserSpace("USER001", 512);

        mockMvc.perform(post("/admin/updateUserSpace")
                .sessionAttr(Constants.SESSION_KEY, adminUser())
                .param("userId", "USER001")
                .param("changeSpace", "512"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 文件管理（管理员） ====================

    @Test
    void loadFileList_shouldReturnList_whenAdmin() throws Exception {
        when(fileInfoService.findListByPage(any())).thenReturn(new PaginationResultVO<>());

        mockMvc.perform(get("/admin/loadFileList")
                .sessionAttr(Constants.SESSION_KEY, adminUser()))
            .andExpect(jsonPath("$.status").value("success"));
    }
}
