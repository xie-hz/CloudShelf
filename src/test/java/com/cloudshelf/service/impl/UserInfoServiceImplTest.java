package com.cloudshelf.service.impl;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SessionWebUserDto;
import com.cloudshelf.entity.dto.SysSettingsDto;
import com.cloudshelf.entity.dto.UserSpaceDto;
import com.cloudshelf.entity.enums.UserStatusEnum;
import com.cloudshelf.entity.po.UserInfo;
import com.cloudshelf.entity.query.UserInfoQuery;
import com.cloudshelf.exception.BusinessException;
import com.cloudshelf.mappers.UserInfoMapper;
import com.cloudshelf.service.EmailCodeService;
import com.cloudshelf.service.FileInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserInfoServiceImpl 单元测试
 * 覆盖: 注册/登录/重置密码/修改用户状态/修改用户空间
 */
@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Mock
    private EmailCodeService emailCodeService;

    @Mock
    private FileInfoService fileInfoService;

    @Mock
    private AppConfig appConfig;

    @Mock
    private RedisComponent redisComponent;

    @InjectMocks
    private UserInfoServiceImpl userInfoService;

    // ==================== 注册 ====================

    @Test
    void register_shouldCreateUser_whenEmailAndNickNameAvailable() {
        when(userInfoMapper.selectByEmail("new@test.com")).thenReturn(null);
        when(userInfoMapper.selectByNickName("新用户")).thenReturn(null);
        when(redisComponent.getSysSettingsDto()).thenReturn(new SysSettingsDto() {{
            setUserInitUseSpace(1024);
        }});
        when(userInfoMapper.insert(any(UserInfo.class))).thenReturn(1);

        userInfoService.register("new@test.com", "新用户", "MyPass123");

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).insert(captor.capture());
        UserInfo saved = captor.getValue();

        assertNotNull(saved.getUserId());
        assertEquals(10, saved.getUserId().length());
        assertEquals("新用户", saved.getNickName());
        assertEquals("new@test.com", saved.getEmail());
        assertNotNull(saved.getPassword());
        assertEquals(UserStatusEnum.ENABLE.getStatus(), saved.getStatus());
        assertNotNull(saved.getJoinTime());
        assertEquals(Long.valueOf(0L), saved.getUseSpace());
    }

    @Test
    void register_shouldThrowException_whenEmailExists() {
        UserInfo existing = new UserInfo();
        existing.setEmail("exist@test.com");
        when(userInfoMapper.selectByEmail("exist@test.com")).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class, () ->
            userInfoService.register("exist@test.com", "新用户", "password"));

        assertEquals("邮箱账号已经存在", ex.getMessage());
        verify(userInfoMapper, never()).insert(any());
    }

    @Test
    void register_shouldThrowException_whenNickNameExists() {
        when(userInfoMapper.selectByEmail("new@test.com")).thenReturn(null);
        UserInfo existing = new UserInfo();
        existing.setNickName("已有昵称");
        when(userInfoMapper.selectByNickName("已有昵称")).thenReturn(existing);

        assertThrows(BusinessException.class, () ->
            userInfoService.register("new@test.com", "已有昵称", "password"));
    }

    // ==================== 登录 ====================

    @Test
    void login_shouldReturnSessionUser_whenCredentialsCorrect() {
        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("USER001");
        dbUser.setNickName("测试用户");
        dbUser.setEmail("test@test.com");
        dbUser.setPassword("hashedPassword");
        dbUser.setStatus(UserStatusEnum.ENABLE.getStatus());
        dbUser.setTotalSpace(1024L * 1024 * 1024);
        when(userInfoMapper.selectByEmail("test@test.com")).thenReturn(dbUser);
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq("USER001"))).thenReturn(1);
        when(fileInfoService.getUserUseSpace("USER001")).thenReturn(100L);
        when(appConfig.getAdminEmails()).thenReturn("admin@qq.com");

        SessionWebUserDto result = userInfoService.login("test@test.com", "hashedPassword");

        assertEquals("USER001", result.getUserId());
        assertEquals("测试用户", result.getNickName());
        assertFalse(result.getAdmin());  // 不是管理员
        verify(redisComponent).saveUserSpaceUse(eq("USER001"), any(UserSpaceDto.class));
    }

    @Test
    void login_shouldSetAdminTrue_whenEmailIsAdmin() {
        UserInfo dbUser = new UserInfo();
        dbUser.setUserId("ADMIN001");
        dbUser.setNickName("管理员");
        dbUser.setEmail("admin@qq.com");
        dbUser.setPassword("adminPass");
        dbUser.setStatus(UserStatusEnum.ENABLE.getStatus());
        dbUser.setTotalSpace(1024L * 1024 * 1024);
        when(userInfoMapper.selectByEmail("admin@qq.com")).thenReturn(dbUser);
        when(userInfoMapper.updateByUserId(any(), eq("ADMIN001"))).thenReturn(1);
        when(fileInfoService.getUserUseSpace("ADMIN001")).thenReturn(0L);
        when(appConfig.getAdminEmails()).thenReturn("admin@qq.com,admin2@qq.com");

        SessionWebUserDto result = userInfoService.login("admin@qq.com", "adminPass");

        assertTrue(result.getAdmin());  // 是管理员
    }

    @Test
    void login_shouldThrowException_whenAccountNotExist() {
        when(userInfoMapper.selectByEmail("no@test.com")).thenReturn(null);

        assertThrows(BusinessException.class, () ->
            userInfoService.login("no@test.com", "password"));
    }

    @Test
    void login_shouldThrowException_whenPasswordWrong() {
        UserInfo dbUser = new UserInfo();
        dbUser.setPassword("correctHash");
        when(userInfoMapper.selectByEmail("test@test.com")).thenReturn(dbUser);

        assertThrows(BusinessException.class, () ->
            userInfoService.login("test@test.com", "wrongPassword"));
    }

    @Test
    void login_shouldThrowException_whenAccountDisabled() {
        UserInfo dbUser = new UserInfo();
        dbUser.setPassword("hashedPassword");
        dbUser.setStatus(UserStatusEnum.DISABLE.getStatus());
        when(userInfoMapper.selectByEmail("disabled@test.com")).thenReturn(dbUser);

        assertThrows(BusinessException.class, () ->
            userInfoService.login("disabled@test.com", "hashedPassword"));
    }

    // ==================== 重置密码 ====================

    @Test
    void resetPwd_shouldUpdatePassword_whenEmailExists() {
        UserInfo dbUser = new UserInfo();
        dbUser.setEmail("test@test.com");
        when(userInfoMapper.selectByEmail("test@test.com")).thenReturn(dbUser);
        when(userInfoMapper.updateByEmail(any(UserInfo.class), eq("test@test.com"))).thenReturn(1);

        userInfoService.resetPwd("test@test.com", "NewPass123");

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateByEmail(captor.capture(), eq("test@test.com"));
        assertNotNull(captor.getValue().getPassword());
    }

    @Test
    void resetPwd_shouldThrowException_whenEmailNotExists() {
        when(userInfoMapper.selectByEmail("no@test.com")).thenReturn(null);

        assertThrows(BusinessException.class, () ->
            userInfoService.resetPwd("no@test.com", "NewPass123"));
    }

    // ==================== 修改用户状态（管理员） ====================

    @Test
    void updateUserStatus_shouldDisableUser_andClearFiles() {
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq("USER001"))).thenReturn(1);

        userInfoService.updateUserStatus("USER001", UserStatusEnum.DISABLE.getStatus());

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateByUserId(captor.capture(), eq("USER001"));
        assertEquals(UserStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(Long.valueOf(0L), captor.getValue().getUseSpace());
        verify(fileInfoService).deleteFileByUserId("USER001");
    }

    @Test
    void updateUserStatus_shouldEnableUser_withoutClearingFiles() {
        when(userInfoMapper.updateByUserId(any(UserInfo.class), eq("USER001"))).thenReturn(1);

        userInfoService.updateUserStatus("USER001", UserStatusEnum.ENABLE.getStatus());

        verify(fileInfoService, never()).deleteFileByUserId(anyString());
    }

    // ==================== 修改用户空间 ====================

    @Test
    void changeUserSpace_shouldUpdateAndResetCache() {
        when(userInfoMapper.updateUserSpace(eq("USER001"), isNull(), eq(512L * Constants.MB))).thenReturn(1);

        userInfoService.changeUserSpace("USER001", 512);

        verify(userInfoMapper).updateUserSpace("USER001", null, 512L * Constants.MB);
        verify(redisComponent).resetUserSpaceUse("USER001");
    }
}
