package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.dto.SysSettingsDto;
import com.cloudshelf.entity.po.UserInfo;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.entity.vo.ResponseVO;
import com.cloudshelf.exception.BusinessException;
import com.cloudshelf.service.EmailCodeService;
import com.cloudshelf.service.FileInfoService;
import com.cloudshelf.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccountController 接口测试
 * 覆盖: 注册/登录/登出/获取用户信息/修改密码/上传头像
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserInfoService userInfoService;

    @MockBean
    private EmailCodeService emailCodeService;

    @MockBean
    private FileInfoService fileInfoService;

    @MockBean
    private RedisComponent redisComponent;

    @MockBean
    private AppConfig appConfig;

    // ==================== 注册 ====================

    @Test
    void register_shouldReturnSuccess_whenValidInput() throws Exception {
        doNothing().when(userInfoService).register(anyString(), anyString(), anyString());

        mockMvc.perform(post("/register")
                .param("email", "test@qq.com")
                .param("nickName", "测试用户")
                .param("password", "MyPass123")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void register_shouldFail_whenCheckCodeWrong() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", "test@qq.com")
                .param("nickName", "测试用户")
                .param("password", "MyPass123")
                .param("checkCode", "WRONG")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void register_shouldReject_whenEmailInvalid() throws Exception {
        // @VerifyParam(regex = EMAIL) 拦截非法邮箱格式
        mockMvc.perform(post("/register")
                .param("email", "not-an-email")
                .param("nickName", "测试用户")
                .param("password", "MyPass123")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    void register_shouldReject_whenPasswordTooShort() throws Exception {
        // @VerifyParam(min = 8) 拦截过短密码
        mockMvc.perform(post("/register")
                .param("email", "test@qq.com")
                .param("nickName", "测试用户")
                .param("password", "123")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    void register_shouldReject_whenRequiredFieldMissing() throws Exception {
        // email 是 required，故意不传
        mockMvc.perform(post("/register")
                .param("nickName", "测试用户")
                .param("password", "12345678")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== 登录 ====================

    @Test
    void login_shouldReturnUserInfo_whenCredentialsValid() throws Exception {
        SessionWebUserDto userDto = new SessionWebUserDto();
        userDto.setUserId("USER001");
        userDto.setNickName("测试用户");
        userDto.setAdmin(false);
        when(userInfoService.login("test@qq.com", "password")).thenReturn(userDto);

        mockMvc.perform(post("/login")
                .param("email", "test@qq.com")
                .param("password", "password")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.nickName").value("测试用户"))
            .andExpect(jsonPath("$.data.userId").value("USER001"));
    }

    @Test
    void login_shouldFail_whenAccountNotExist() throws Exception {
        when(userInfoService.login(eq("no@qq.com"), anyString()))
            .thenThrow(new BusinessException("账号或者密码错误"));

        mockMvc.perform(post("/login")
                .param("email", "no@qq.com")
                .param("password", "wrong")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.status").value("error"));
    }

    // ==================== 获取用户信息（需登录） ====================

    @Test
    void getUserInfo_shouldReturnUser_whenLoggedIn() throws Exception {
        SessionWebUserDto userDto = new SessionWebUserDto();
        userDto.setUserId("USER001");
        userDto.setNickName("测试用户");
        userDto.setAdmin(false);

        mockMvc.perform(get("/getUserInfo")
                .sessionAttr(Constants.SESSION_KEY, userDto))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.nickName").value("测试用户"));
    }

    @Test
    void getUserInfo_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/getUserInfo"))
            .andExpect(jsonPath("$.code").value(901));
    }

    // ==================== 退出登录 ====================

    @Test
    void logout_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/logout"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 修改密码（需登录） ====================

    @Test
    void updatePassword_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/updatePassword")
                .param("password", "NewPass123"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void updatePassword_shouldReject_whenPasswordTooShort() throws Exception {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/updatePassword")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("password", "123"))
            .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== 获取已使用空间 ====================

    @Test
    void getUseSpace_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/getUseSpace"))
            .andExpect(jsonPath("$.code").value(901));
    }
}
