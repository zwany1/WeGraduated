package com.graduate.thesis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.service.captcha.CaptchaData;
import com.graduate.thesis.service.captcha.CaptchaGenerator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务: 随机调度点选/滑动/旋转三种类型, 内存存储, 一次性校验, 5 分钟过期
 */
@Service
public class CaptchaService {

    private static final int EXPIRE_MILLIS = 5 * 60 * 1000;

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final List<CaptchaGenerator> generators;
    private final ObjectMapper objectMapper;

    public CaptchaService(List<CaptchaGenerator> generators, ObjectMapper objectMapper) {
        this.generators = generators;
        this.objectMapper = objectMapper;
    }

    /** 生成验证码, 返回 {captchaId, type, render} */
    public CaptchaResult generate() {
        cleanup(); // 请求级清扫: 免登录接口, 防止两次定时清理之间被刷爆内存
        CaptchaGenerator gen = generators.get(ThreadLocalRandom.current().nextInt(generators.size()));
        CaptchaData data = gen.generate();
        String captchaId = Long.toHexString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE);
        store.put(captchaId, new Entry(data, System.currentTimeMillis() + EXPIRE_MILLIS));
        return new CaptchaResult(captchaId, data.type, data.render);
    }

    /** 校验验证码(一次性消费, 失败也消费, 防枚举) */
    public void verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null || captchaCode.trim().isEmpty()) {
            throw new BusinessException("请先获取图形验证码");
        }
        Entry entry = store.remove(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            throw new BusinessException("图形验证码已过期，请刷新重试");
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(captchaCode);
        } catch (Exception e) {
            throw new BusinessException("验证码格式错误");
        }
        String type = payload.path("type").asText();
        CaptchaGenerator gen = findGenerator(type);
        if (gen == null) {
            throw new BusinessException("验证码类型无效");
        }
        if (!gen.verify(entry.data.secret, payload)) {
            throw new BusinessException("图形验证码错误");
        }
    }

    private CaptchaGenerator findGenerator(String type) {
        for (CaptchaGenerator g : generators) {
            if (g.type().equals(type)) {
                return g;
            }
        }
        return null;
    }

    /** 每 10 分钟清理过期验证码, 防止内存累积 */
    @Scheduled(fixedDelay = 600000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    private static class Entry {
        final CaptchaData data;
        final long expireAt;
        Entry(CaptchaData data, long expireAt) {
            this.data = data;
            this.expireAt = expireAt;
        }
    }

    public static class CaptchaResult {
        public final String captchaId;
        public final String type;
        public final Map<String, Object> render;
        CaptchaResult(String captchaId, String type, Map<String, Object> render) {
            this.captchaId = captchaId;
            this.type = type;
            this.render = render;
        }
    }
}
