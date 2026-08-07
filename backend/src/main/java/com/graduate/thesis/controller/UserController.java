package com.graduate.thesis.controller;

import com.graduate.thesis.common.Result;
import com.graduate.thesis.dto.LoginResponse;
import com.graduate.thesis.dto.UserAuthDTO;
import com.graduate.thesis.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
