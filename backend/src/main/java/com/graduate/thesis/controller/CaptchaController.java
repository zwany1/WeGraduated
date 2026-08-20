package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.service.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码接口: 点选/滑动/旋转三种类型随机
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/generate")
    public Result<Map<String, Object>> generate() {
        CaptchaService.CaptchaResult r = captchaService.generate();
        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", r.captchaId);
        data.put("type", r.type);
        if (r.render != null) {
            data.putAll(r.render);
        }
        return Result.ok(data);
    }
}
