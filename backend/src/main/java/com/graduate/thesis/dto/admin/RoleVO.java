package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色视图
 */
@Data
public class RoleVO {

    private Long id;

    private String roleName;

    private String roleKey;

    private String remark;

    private Boolean status;

    private Integer userCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
