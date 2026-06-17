package com.cloudshelf.aspect;

import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalOperationAspect AOP 切面集成测试
 * 覆盖: 参数校验 / 登录拦截 / 管理员权限
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class GlobalOperationAspectTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== 参数校验 ====================

    @Test
    void shouldReject_whenRequiredParamMissing() throws Exception {
        mockMvc.perform(post("/register")
                .param("nickName", "测试")
                .param("password", "12345678")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    void shouldReject_whenParamTooLong() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 151; i++) sb.append("a");
        String tooLong = sb.toString();
        mockMvc.perform(post("/register")
                .param("email", tooLong + "@test.com")
                .param("nickName", "测试")
                .param("password", "12345678")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    void shouldReject_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", "test@qq.com")
                .param("nickName", "测试")
                .param("password", "123")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    @Test
    void shouldReject_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", "not-an-email")
                .param("nickName", "测试")
                .param("password", "12345678")
                .param("checkCode", "ABCD")
                .sessionAttr(Constants.CHECK_CODE_KEY, "ABCD"))
            .andExpect(jsonPath("$.code").value(600));
    }

    // ==================== 登录拦截 ====================

    @Test
    void shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/getUserInfo"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void shouldAllow_whenLoggedIn() throws Exception {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");
        user.setNickName("测试");

        mockMvc.perform(get("/getUserInfo")
                .sessionAttr(Constants.SESSION_KEY, user))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 管理员权限 ====================

    @Test
    void shouldReject_whenNotAdmin() throws Exception {
        SessionWebUserDto normal = new SessionWebUserDto();
        normal.setUserId("USER001");
        normal.setAdmin(false);

        mockMvc.perform(get("/admin/getSysSettings")
                .sessionAttr(Constants.SESSION_KEY, normal))
            .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldAllow_whenAdmin() throws Exception {
        SessionWebUserDto admin = new SessionWebUserDto();
        admin.setUserId("ADMIN001");
        admin.setAdmin(true);

        mockMvc.perform(get("/admin/getSysSettings")
                .sessionAttr(Constants.SESSION_KEY, admin))
            .andExpect(jsonPath("$.status").value("success"));
    }

    // ==================== 分享页面无需登录 ====================

    @Test
    void shouldAllowWithoutLogin_whenCheckLoginFalse() throws Exception {
        // /showShare/getShareInfo 注解 @GlobalInterceptor(checkLogin = false)
        mockMvc.perform(get("/showShare/getShareInfo")
                .param("shareId", "SHARE001"))
            .andExpect(jsonPath("$.status").value("error"));  // 业务异常非 901
    }
}
