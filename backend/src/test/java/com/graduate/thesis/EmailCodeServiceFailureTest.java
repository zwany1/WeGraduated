package com.graduate.thesis;

import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.service.EmailCodeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * 邮箱验证码失败提示分类测试。
 *
 * <p>背景: 发件人(spring.mail.username)为空时 SimpleMailMessage.setFrom("") 抛
 * AddressException, 旧实现统一提示"邮件发送失败，请检查邮箱地址"，导致用户以为是
 * 自己填的 QQ/163 邮箱有问题，实际是服务端未配置。这里锁定各类故障的提示文案。</p>
 */
class EmailCodeServiceFailureTest {

    private static final String TO = "someone@example.com";

    private EmailCodeService service(JavaMailSender sender, String from, String password) {
        return new EmailCodeService(sender, from, password);
    }

    /** 未配置发件人时，必须报"未配置"而不是让用户检查自己的邮箱 */
    @Test
    void 发件人为空时提示邮件服务未配置() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        EmailCodeService svc = service(sender, "", "pwd");
        assertFalse(svc.isConfigured());

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_NOT_CONFIGURED, ex.getMessage());
        // 未配置时不得触碰 SMTP，也不应记录验证码
        Mockito.verify(sender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    /** 发件人有默认值但授权码为空，同样属于未配置 */
    @Test
    void 授权码为空时提示邮件服务未配置() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        EmailCodeService svc = service(sender, "2651896126@qq.com", "");
        assertFalse(svc.isConfigured());

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_NOT_CONFIGURED, ex.getMessage());
        Mockito.verify(sender, Mockito.never()).send(any(SimpleMailMessage.class));
    }

    /** 配置完整时正常发送，并记录验证码 */
    @Test
    void 配置完整时正常发送成功() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        EmailCodeService svc = service(sender, "2651896126@qq.com", "auth-code");
        assertTrue(svc.isConfigured());

        svc.sendCode(TO);
        Mockito.verify(sender, Mockito.times(1)).send(any(SimpleMailMessage.class));
        // 同一邮箱 60 秒内重复请求应被限流拦截
        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertTrue(ex.getMessage().contains("发送过于频繁"), ex.getMessage());
    }

    /** SMTP 535 认证失败: 授权码过期，属于管理员职责 */
    @Test
    void 认证失败时提示联系管理员() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        Mockito.doThrow(new MailAuthenticationException("535 Error: authentication failed"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailCodeService svc = service(sender, "2651896126@qq.com", "bad-code");

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_AUTH_FAILED, ex.getMessage());
    }

    /** 连接不上 SMTP: 网络/端口问题，不是用户的邮箱问题 */
    @Test
    void 连接失败时提示稍后重试() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        // Spring 把连接故障包进 MailSendException, 需靠消息下钻识别
        Mockito.doThrow(new MailSendException("Mail server connection failed; "
                + "nested exception is javax.mail.MessagingException: Couldn't connect to host, port: smtp.qq.com, 465"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailCodeService svc = service(sender, "2651896126@qq.com", "auth-code");

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_CONNECT_FAILED, ex.getMessage());
    }

    /** 对方邮箱拒收(550): 这才是真正需要用户检查邮箱地址的场景 */
    @Test
    void 对方拒收时提示检查邮箱地址() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        Mockito.doThrow(new MailSendException("550 Mailbox not found"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailCodeService svc = service(sender, "2651896126@qq.com", "auth-code");

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_ADDRESS_REJECTED, ex.getMessage());
    }

    /** 收件地址解析失败 */
    @Test
    void 地址解析失败时提示格式不正确() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        Mockito.doThrow(new MailParseException("Illegal address"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailCodeService svc = service(sender, "2651896126@qq.com", "auth-code");

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertTrue(ex.getMessage().contains("邮箱地址格式不正确"), ex.getMessage());
    }

    /** 未知故障兜底，且不得把原始异常堆栈泄漏给用户 */
    @Test
    void 未知异常使用兜底提示且不泄漏内部细节() {
        JavaMailSender sender = Mockito.mock(JavaMailSender.class);
        Mockito.doThrow(new MailSendException("something weird"))
                .when(sender).send(any(SimpleMailMessage.class));
        EmailCodeService svc = service(sender, "2651896126@qq.com", "auth-code");

        BusinessException ex = assertThrows(BusinessException.class, () -> svc.sendCode(TO));
        assertEquals(EmailCodeService.MSG_UNKNOWN, ex.getMessage());
        assertFalse(ex.getMessage().contains("weird"), ex.getMessage());
    }
}
