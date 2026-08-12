package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 邮箱验证码服务(内存存储, 5 分钟过期, 60 秒发送限流)
 */
@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);

    private static final long CODE_EXPIRE_MILLIS = 5 * 60 * 1000;
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000;

    private final JavaMailSender mailSender;
    private final String from;

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

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

    /** 发送验证码到指定邮箱 */
    public void sendCode(String email) {
        String key = email.toLowerCase();
        Entry existing = store.get(key);
        if (existing != null && System.currentTimeMillis() - existing.sentAt < SEND_INTERVAL_MILLIS) {
            long remain = 60 - (System.currentTimeMillis() - existing.sentAt) / 1000;
            throw new BusinessException("发送过于频繁，请 " + remain + " 秒后再试");
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
}
