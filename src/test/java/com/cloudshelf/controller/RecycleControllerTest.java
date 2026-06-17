package com.cloudshelf.controller;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.vo.PaginationResultVO;
import com.cloudshelf.service.FileInfoService;
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
 * RecycleController 接口测试
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RecycleControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FileInfoService fileInfoService;
    @MockBean private RedisComponent redisComponent;
    @MockBean private AppConfig appConfig;

    @Test
    void loadRecycleList_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/recycle/loadRecycleList"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void loadRecycleList_shouldReturnList_whenLoggedIn() throws Exception {
        when(fileInfoService.findListByPage(any())).thenReturn(new PaginationResultVO<>());
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(get("/recycle/loadRecycleList")
                .sessionAttr(Constants.SESSION_KEY, user))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void recoverFile_shouldSucceed_whenLoggedIn() throws Exception {
        SessionWebUserDto user = new SessionWebUserDto();
        user.setUserId("USER001");

        mockMvc.perform(post("/recycle/recoverFile")
                .sessionAttr(Constants.SESSION_KEY, user)
                .param("fileIds", "FILE001"))
            .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void recoverFile_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/recycle/recoverFile").param("fileIds", "FILE001"))
            .andExpect(jsonPath("$.code").value(901));
    }

    @Test
    void delFile_shouldReturn901_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/recycle/delFile").param("fileIds", "FILE001"))
            .andExpect(jsonPath("$.code").value(901));
    }
}
