package com.graduate.thesis.service.captcha;

import java.util.Map;

/**
 * 验证码数据载体: render 供前端渲染, secret 仅供后端校验(不下发)
 */
public class CaptchaData {

    public final String type;
    public final Map<String, Object> render;
    public final Object secret;

    public CaptchaData(String type, Map<String, Object> render, Object secret) {
        this.type = type;
        this.render = render;
        this.secret = secret;
    }
}
