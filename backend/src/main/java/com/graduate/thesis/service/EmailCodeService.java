package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final JavaMailSender mailSender;
    private final String from;

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
                            @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
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

    /** 发送验证码到指定邮箱 */
    public void sendCode(String email) {
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
            log.error("发送邮件失败", e);
            // 发送失败回补 IP 限流额度, 避免 SMTP 抖动误伤同网络用户
            if (ip != null) {
                releaseIpSlot(ip);
            }
            throw new BusinessException("邮件发送失败，请检查邮箱地址或稍后重试");
        }
        store.put(key, new Entry(code, System.currentTimeMillis() + CODE_EXPIRE_MILLIS,
                System.currentTimeMillis()));
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
