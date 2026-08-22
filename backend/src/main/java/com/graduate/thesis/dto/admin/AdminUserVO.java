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

    /** 头像(data URL base64) */
    private String avatar;

    private String role;

    /** 状态: 1正常 0禁用 */
    private Boolean status;

    /** 角色ID列表(RBAC) */
    private List<Long> roleIds;

    /** 角色名称列表(RBAC) */
    private List<String> roleNames;

    private LocalDateTime createTime;

    private long templateCount;

    private long taskCount;

    private long paperCount;
}
