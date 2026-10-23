package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邮箱验证码服务(内存存储, 5 分钟过期, 邮箱 60 秒 + IP 每小时限量双重限流)
 *
 * <p>失败提示按异常类型分类, 不再对所有失败统一提示"请检查邮箱地址":
 * 未配置 / 认证失败 / 连接失败属于服务端问题, 应引导联系管理员;
 * 只有收件地址被对方拒收时才提示用户检查邮箱。</p>
 */
@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);

    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000;
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000;
    /** 单 IP 每小时最多发送次数, 防止换邮箱轰炸 SMTP */
    private static final int IP_LIMIT_PER_HOUR = 10;
    private static final long IP_WINDOW_MILLIS = 60 * 60 * 1000;
    /** 验证码存储 Map 容量上限, 超限清理防止内存累积 */
    private static final int CODE_STORE_MAX = 10000;

    /** 服务端配置缺失: 引导联系管理员, 而非让用户自查邮箱 */
    public static final String MSG_NOT_CONFIGURED = "邮件服务未配置，请联系管理员开启邮箱验证码功能";
    public static final String MSG_AUTH_FAILED = "邮件服务认证失败，请联系管理员检查邮箱服务配置";
    public static final String MSG_CONNECT_FAILED = "邮件服务连接失败，请稍后再试或联系管理员";
    public static final String MSG_ADDRESS_REJECTED = "该邮箱地址无法送达，请检查邮箱地址是否正确";
    public static final String MSG_UNKNOWN = "邮件发送失败，请稍后重试";

    private final JavaMailSender mailSender;
    private final String from;
    private final String password;

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    /** IP 限流窗口: ip -> [窗口起点ms, 已发送次数] */
    private final ConcurrentHashMap<String, long[]> ipWindow = new ConcurrentHashMap<>();

    private static class Entry {
        final String code;
        final long expireAt;
        final long sentAt;
        Entry(String code, long expireAt, long sentAt) {
            this.code = code;
            this.expireAt = expireAt;
            this.sentAt = sentAt;
        }
    }

    public EmailCodeService(JavaMailSender mailSender,
                            @Value("${spring.mail.username:}") String from,
                            @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.from = from == null ? "" : from.trim();
        this.password = password == null ? "" : password.trim();
        if (this.from.isEmpty()) {
            // 发件人为空时 SimpleMailMessage.setFrom("") 抛 AddressException,
            // 若不加提示, 运维只会看到"邮件发送失败", 极难定位到配置缺失。
            log.warn("邮件服务未配置发件人(spring.mail.username 为空)，邮箱验证码功能不可用，"
                    + "请设置环境变量 MAIL_USERNAME");
        } else if (this.password.isEmpty()) {
            log.warn("邮件服务未配置 SMTP 授权码(spring.mail.password 为空)，发送时会出现认证失败，"
                    + "请设置环境变量 MAIL_PASSWORD");
        }
    }

    /** 清理过期验证码与超限条目, 防止内存累积 */
    private void sweepCodes() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
        ipWindow.entrySet().removeIf(e -> now - e.getValue()[0] > IP_WINDOW_MILLIS);
        if (store.size() > CODE_STORE_MAX) {
            store.clear();
        }
        if (ipWindow.size() > CODE_STORE_MAX) {
            ipWindow.clear();
        }
    }

    /** 邮件服务是否已完整配置(发件人与授权码均非空) */
    public boolean isConfigured() {
        return !from.isEmpty() && !password.isEmpty();
    }

    /** 发送验证码到指定邮箱 */
    public void sendCode(String email) {
        // 配置缺失属于服务端问题, 必须在限流与 SMTP 调用之前拦截:
        // 否则用户会看到"请检查邮箱地址"的误导性提示, 且白占一次限流额度。
        if (from.isEmpty() || password.isEmpty()) {
            log.error("拒绝发送验证码: 邮件服务配置不完整(from={}, password={})",
                    from.isEmpty() ? "空" : "已配置", password.isEmpty() ? "空" : "已配置");
            throw new BusinessException(MSG_NOT_CONFIGURED);
        }
        sweepCodes();
        String key = email.toLowerCase();
        Entry existing = store.get(key);
        if (existing != null && System.currentTimeMillis() - existing.sentAt < SEND_INTERVAL_MILLIS) {
            long remain = 60 - (System.currentTimeMillis() - existing.sentAt) / 1000;
            throw new BusinessException("发送过于频繁，请 " + remain + " 秒后再试");
        }
        String ip = clientIp();
        if (ip != null && !acquireIpSlot(ip)) {
            throw new BusinessException("该网络环境请求过于频繁，请稍后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(email);
            msg.setSubject("论文格式助手 - 邮箱验证码");
            msg.setText("你的验证码是：" + code + "，5 分钟内有效。若非本人操作请忽略。");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("发送邮件失败, to={}", email, e);
            // 发送失败回补 IP 限流额度, 避免 SMTP 抖动误伤同网络用户
            if (ip != null) {
                releaseIpSlot(ip);
            }
            throw new BusinessException(classify(e));
        }
        store.put(key, new Entry(code, System.currentTimeMillis() + CODE_EXPIRE_MILLIS,
                System.currentTimeMillis()));
    }

    /**
     * 按异常类型映射到面向用户的提示。
     * 服务端故障(认证/连接)引导联系管理员, 只有收件方拒收才让用户自查邮箱地址。
     */
    private String classify(Exception e) {
        if (e instanceof MailAuthenticationException) {
            return MSG_AUTH_FAILED;
        }
        if (e instanceof MailParseException) {
            // 发件人已前置校验, 此处只可能是收件地址解析失败
            return "邮箱地址格式不正确，请检查后重新填写";
        }
        if (e instanceof MailSendException) {
            // Spring 会把 SMTP 协议错误包进 MailSendException, 需下钻判断到底是
            // 认证问题(535)还是对方拒收(550/553), 二者的处理人完全不同。
            String detail = nestedDetail(e);
            if (detail.contains("535") || detail.contains("AUTHENTICATION FAILED")
                    || detail.contains("AUTHENTICATION UNSUCCESSFUL")
                    || detail.contains("AUTH LOGIN")
                    || detail.contains("BAD CREDENTIALS")) {
                return MSG_AUTH_FAILED;
            }
            if (detail.contains("550") || detail.contains("553") || detail.contains("554")
                    || detail.contains("551") || detail.contains("552")
                    || detail.contains("MAILBOX") || detail.contains("NO SUCH USER")) {
                return MSG_ADDRESS_REJECTED;
            }
            if (detail.contains("TIMED OUT") || detail.contains("TIMEOUT")
                    || detail.contains("COULDN'T CONNECT") || detail.contains("COULD NOT CONNECT")
                    || detail.contains("CONNECTION REFUSED")
                    || detail.contains("MAIL SERVER CONNECTION FAILED")
                    || detail.contains("UNKNOWNHOST")) {
                return MSG_CONNECT_FAILED;
            }
            return MSG_UNKNOWN;
        }
        return MSG_UNKNOWN;
    }

    /** 拼接异常链上的全部消息文本, 用于识别 SMTP 协议错误码 */
    private String nestedDetail(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            String m = c.getMessage();
            if (m != null) {
                sb.append(' ').append(m);
            }
            if (c instanceof MailSendException) {
                // MailSendException 的失败明细挂在 failedMessages 上, 不在 cause 链里
                for (Object o : ((MailSendException) c).getFailedMessages().keySet()) {
                    sb.append(' ').append(String.valueOf(o));
                }
            }
            if (sb.length() > 2000) {
                break;
            }
        }
        return sb.toString().toUpperCase();
    }

    /** 校验验证码(一次性) */
    public void verify(String email, String code) {
        if (code == null || code.isEmpty()) {
            throw new BusinessException("请输入邮箱验证码");
        }
        String key = email.toLowerCase();
        Entry entry = store.get(key);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            store.remove(key);
            throw new BusinessException("邮箱验证码已过期，请重新获取");
        }
        if (!entry.code.equals(code.trim())) {
            store.remove(key);
            throw new BusinessException("邮箱验证码错误");
        }
        store.remove(key);
    }

    /** 取当前请求客户端 IP */
    private String clientIp() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return com.graduate.thesis.util.IpUtils.clientIp(req);
        } catch (Exception e) {
            return null;
        }
    }

    /** IP 滑动窗口限流: 每小时 IP_LIMIT_PER_HOUR 次, 超限拒绝 */
    private boolean acquireIpSlot(String ip) {
        long now = System.currentTimeMillis();
        long[] slot = ipWindow.compute(ip, (k, v) -> {
            if (v == null || now - v[0] >= IP_WINDOW_MILLIS) {
                return new long[]{now, 1};
            }
            v[1] = v[1] + 1;
            return v;
        });
        if (slot[1] > IP_LIMIT_PER_HOUR) {
            ipWindow.entrySet().removeIf(e -> now - e.getValue()[0] >= IP_WINDOW_MILLIS);
            return false;
        }
        return true;
    }

    /** 回补一次 IP 限流额度(发送失败时) */
    private void releaseIpSlot(String ip) {
        long[] slot = ipWindow.get(ip);
        if (slot != null && slot[1] > 0) {
            slot[1] = slot[1] - 1;
        }
    }
}
