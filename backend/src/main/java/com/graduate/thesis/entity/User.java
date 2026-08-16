package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("t_user")
public class User {

    /** 普通用户 */
    public static final String ROLE_USER = "USER";
    /** 管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    /** 邮箱 */
    private String email;

    private String nickname;

    /** 头像(data URL base64) */
    private String avatar;

    /** 第三方登录: GitHub 用户 id */
    private String githubId;

    /** 第三方登录: GitHub 登录名 */
    private String githubLogin;

    /** 角色: USER / ADMIN */
    private String role;

    /** 状态: 1正常 0禁用 */
    private Boolean status;

    private LocalDateTime createTime;
}
