package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.ForgotPasswordDTO;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.dto.UserProfileDTO;
import com.graduate.thesis.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody UserAuthDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody UserAuthDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /** 忘记密码: 密保问答重置并重新签发 token */
    @PostMapping("/forgot-password")
    public Result<LoginResponse> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        return Result.ok(userService.forgotPassword(dto));
    }

    /** 退出登录: 撤销当前 token */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = authHeader;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        userService.logout(UserContext.get(), token);
        return Result.ok(null);
    }

    /** 获取当前用户资料 */
    @GetMapping("/profile")
    public Result<UserProfileDTO> profile() {
        return Result.ok(userService.getProfile(UserContext.get()));
    }

    /** 更新当前用户资料(昵称/头像/密保) */
    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@RequestBody UserProfileDTO dto) {
        return Result.ok(userService.updateProfile(UserContext.get(), dto));
    }
}
