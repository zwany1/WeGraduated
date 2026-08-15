package com.graduate.thesis.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前用户权限信息(登录后/刷新页面时获取)
 */
@Data
public class UserInfoResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    /** 主角色: USER / ADMIN */
    private String role;
    /** 角色权限字符串列表(RBAC) */
    private List<String> roles;
    /** 权限标识集合(按钮级) */
    private Set<String> perms;
}
