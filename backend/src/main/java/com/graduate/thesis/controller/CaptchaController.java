package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.service.CaptchaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 图形验证码接口
 */
@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/generate")
    public Result<Map<String, String>> generate() {
        CaptchaService.CaptchaResult r = captchaService.generate();
        Map<String, String> data = new HashMap<>();
        data.put("captchaId", r.captchaId);
        data.put("imageBase64", r.imageBase64);
        return Result.ok(data);
    }
}
