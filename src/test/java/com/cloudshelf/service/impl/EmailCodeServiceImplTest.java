package com.cloudshelf.service.impl;

import com.cloudshelf.component.RedisComponent;
import com.cloudshelf.entity.config.AppConfig;
import com.cloudshelf.entity.constants.Constants;
import com.cloudshelf.entity.dto.SysSettingsDto;
import com.cloudshelf.entity.po.EmailCode;
import com.cloudshelf.entity.po.UserInfo;
import com.cloudshelf.entity.query.EmailCodeQuery;
import com.cloudshelf.entity.query.UserInfoQuery;
import com.cloudshelf.exception.BusinessException;
import com.cloudshelf.mappers.EmailCodeMapper;
import com.cloudshelf.mappers.UserInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmailCodeServiceImpl 单元测试
 * 覆盖: 发送验证码 / 校验验证码
 */
@ExtendWith(MockitoExtension.class)
class EmailCodeServiceImplTest {

    @Mock private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;
    @Mock private JavaMailSender javaMailSender;
    @Mock private AppConfig appConfig;
    @Mock private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Mock private RedisComponent redisComponent;

    @InjectMocks
    private EmailCodeServiceImpl emailCodeService;

    // ==================== 发送验证码（注册） ====================

    @Test
    void sendEmailCode_shouldSend_whenEmailNotRegistered() {
        // type=0 表示注册，需要校验邮箱未被注册
        when(userInfoMapper.selectByEmail("new@test.com")).thenReturn(null);
        when(redisComponent.getSysSettingsDto()).thenReturn(new SysSettingsDto() {{
            setRegisterEmailTitle("验证码");
            setRegisterEmailContent("您的验证码是：%s");
        }});
        when(appConfig.getSendUserName()).thenReturn("sender@qq.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doNothing().when(emailCodeMapper).disableEmailCode("new@test.com");
        when(emailCodeMapper.insert(any(EmailCode.class))).thenReturn(1);

        emailCodeService.sendEmailCode("new@test.com", 0);

        verify(userInfoMapper).selectByEmail("new@test.com");
        verify(emailCodeMapper).insert(any(EmailCode.class));
    }

    @Test
    void sendEmailCode_shouldThrowException_whenEmailAlreadyRegistered() {
        UserInfo existing = new UserInfo();
        existing.setEmail("exist@test.com");
        when(userInfoMapper.selectByEmail("exist@test.com")).thenReturn(existing);

        assertThrows(BusinessException.class, () ->
            emailCodeService.sendEmailCode("exist@test.com", 0));
    }

    @Test
    void sendEmailCode_shouldSkipEmailCheck_whenTypeIsNotZero() {
        // type != 0 时不检查邮箱是否已注册（如找回密码）
        when(redisComponent.getSysSettingsDto()).thenReturn(new SysSettingsDto() {{
            setRegisterEmailTitle("验证码");
            setRegisterEmailContent("您的验证码是：%s");
        }});
        when(appConfig.getSendUserName()).thenReturn("sender@qq.com");
        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doNothing().when(emailCodeMapper).disableEmailCode("any@test.com");
        when(emailCodeMapper.insert(any(EmailCode.class))).thenReturn(1);

        // type=1 表示找回密码等场景，不校验邮箱是否已注册
        assertDoesNotThrow(() -> emailCodeService.sendEmailCode("any@test.com", 1));
        verify(userInfoMapper, never()).selectByEmail(anyString());
    }

    // ==================== 校验验证码 ====================

    @Test
    void checkCode_shouldPass_whenCodeValid() {
        EmailCode dbCode = new EmailCode();
        dbCode.setCode("12345");
        dbCode.setStatus(0);  // 未使用
        dbCode.setCreateTime(new Date());  // 刚创建
        when(emailCodeMapper.selectByEmailAndCode("test@test.com", "12345")).thenReturn(dbCode);

        // 不抛异常即通过
        assertDoesNotThrow(() -> emailCodeService.checkCode("test@test.com", "12345"));
        verify(emailCodeMapper).disableEmailCode("test@test.com");
    }

    @Test
    void checkCode_shouldThrowException_whenCodeNotFound() {
        when(emailCodeMapper.selectByEmailAndCode("test@test.com", "wrong")).thenReturn(null);

        assertThrows(BusinessException.class, () ->
            emailCodeService.checkCode("test@test.com", "wrong"));
    }

    @Test
    void checkCode_shouldThrowException_whenCodeAlreadyUsed() {
        EmailCode dbCode = new EmailCode();
        dbCode.setCode("12345");
        dbCode.setStatus(1);  // 已使用
        dbCode.setCreateTime(new Date());
        when(emailCodeMapper.selectByEmailAndCode("test@test.com", "12345")).thenReturn(dbCode);

        assertThrows(BusinessException.class, () ->
            emailCodeService.checkCode("test@test.com", "12345"));
    }

    @Test
    void checkCode_shouldThrowException_whenCodeExpired() {
        EmailCode dbCode = new EmailCode();
        dbCode.setCode("12345");
        dbCode.setStatus(0);
        // 创建时间为 20 分钟前（超过 15 分钟有效期）
        dbCode.setCreateTime(new Date(System.currentTimeMillis() - 20 * 60 * 1000));
        when(emailCodeMapper.selectByEmailAndCode("test@test.com", "12345")).thenReturn(dbCode);

        assertThrows(BusinessException.class, () ->
            emailCodeService.checkCode("test@test.com", "12345"));
    }
}
