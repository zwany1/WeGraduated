package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    /** 角色ID列表(RBAC) */
    private List<Long> roleIds;

    /** 角色名称列表(RBAC) */
    private List<String> roleNames;

    private LocalDateTime createTime;

    private long templateCount;

    private long taskCount;

    private long paperCount;
}
