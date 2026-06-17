package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.po.FileShare;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.service.FileShareService;
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
 * ShareController 接口测试
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileShareService fileShareService;
    @MockBean private RedisComponent redisComponent;
    @MockBean private AppConfig appConfig;

    @Test
    void loadShareList_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/share/loadShareList"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void loadShareList_shouldReturnList_whenLoggedIn() throws Exception {
        when(fileShareService.findListByPage(any())).thenReturn(new PaginationResultVO<>());
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(get("/share/loadShareList")
                .sessionAttr(Constants.SESSION_KEY, user))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void shareFile_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/share/shareFile")
                .param("fileId", "FILE001")
                .param("validType", "0"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void shareFile_shouldCreateShare_whenLoggedIn() throws Exception {
        doNothing().when(fileShareService).saveShare(any(FileShare.class));
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/share/shareFile")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("fileId", "FILE001")
                .param("validType", "0")
                .param("code", "ABCDE"))
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.data.fileId").value("FILE001"));
    }

    @Test
    void cancelShare_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/share/cancelShare").param("shareIds", "S001"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void cancelShare_shouldSucceed_whenLoggedIn() throws Exception {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/share/cancelShare")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("shareIds", "S001,S002"))
            .andExpect(jsonPath("$.status").value("success"));
    }
}
