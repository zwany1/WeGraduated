package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
}
