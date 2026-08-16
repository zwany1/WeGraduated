package com.graduate.thesis.dto;

import lombok.Data;

/**
 * 用户资料
 */
@Data
public class UserProfileDTO {
    private Long userId;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
}
