package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户行
 */
@Data
public class AdminUserVO {

    private Long id;

    private String username;

    private String email;

    private String nickname;

    private String role;

    private LocalDateTime createTime;

    private long templateCount;

    private long taskCount;

    private long paperCount;
}
