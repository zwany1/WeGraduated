package com.graduate.thesis.service;

import com.graduate.thesis.common.BusinessException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 图形验证码(内存存储, 一次性, 60 秒过期)
 */
@Service
public class CaptchaService {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 44;
    private static final int EXPIRE_MILLIS = 60 * 1000;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    private static class Entry {
        final String code;
        final long expireAt;
        Entry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    /** 生成验证码, 返回 {captchaId, imageBase64} */
    public CaptchaResult generate() {
        String code = randomCode(4);
        String captchaId = Long.toHexString(ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE);
        store.put(captchaId, new Entry(code, System.currentTimeMillis() + EXPIRE_MILLIS));
        String image = drawImage(code);
        return new CaptchaResult(captchaId, image);
    }

    /** 校验验证码(一次性) */
    public void verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            throw new BusinessException("请先获取图形验证码");
        }
        Entry entry = store.get(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            store.remove(captchaId);
            throw new BusinessException("图形验证码已过期，请刷新重试");
        }
        if (!entry.code.equalsIgnoreCase(code.trim())) {
            store.remove(captchaId);
            throw new BusinessException("图形验证码错误");
        }
        store.remove(captchaId);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String drawImage(String code) {
        try {
            BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            Random r = ThreadLocalRandom.current();
            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(r.nextInt(160), r.nextInt(160), r.nextInt(160), 120));
                g.drawLine(r.nextInt(WIDTH), r.nextInt(HEIGHT), r.nextInt(WIDTH), r.nextInt(HEIGHT));
            }
            // 字符
            Font font = new Font("Arial", Font.BOLD, 26);
            g.setFont(font);
            int x = 14;
            for (char c : code.toCharArray()) {
                g.setColor(new Color(r.nextInt(80), r.nextInt(100), 180));
                g.drawString(String.valueOf(c), x, 32 + (r.nextInt(6) - 3));
                x += 24;
            }
            // 噪点
            for (int i = 0; i < 50; i++) {
                g.setColor(new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200), 150));
                g.fillRect(r.nextInt(WIDTH), r.nextInt(HEIGHT), 1, 1);
            }
            g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("图形验证码生成失败");
        }
    }

    public static class CaptchaResult {
        public final String captchaId;
        public final String imageBase64;
        CaptchaResult(String captchaId, String imageBase64) {
            this.captchaId = captchaId;
            this.imageBase64 = imageBase64;
        }
    }
}
