package com.graduate.thesis.service.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import com.graduate.thesis.common.BusinessException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 验证码生成器接口: 点选/滑动/旋转 各一实现, 纯 java.awt 手绘, 不引入第三方依赖
 */
public interface CaptchaGenerator {

    /** 类型标识: CLICK / SLIDER / ROTATE */
    String type();

    /** 生成验证码(图片 + 前端渲染字段 + 后端校验秘密) */
    CaptchaData generate();

    /** 校验前端提交的 payload(已反序列化为 JsonNode) */
    boolean verify(Object secret, JsonNode payload);

    /** BufferedImage -> base64 PNG(无 data: 前缀) */
    static String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("验证码图片生成失败");
        }
    }
}
