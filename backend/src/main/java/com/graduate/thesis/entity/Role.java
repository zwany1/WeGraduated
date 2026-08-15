package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色
 */
@Data
@TableName("t_role")
public class Role {

    /** 超管角色权限字符串 */
    public static final String KEY_ADMIN = "admin";
    /** 普通用户角色权限字符串 */
    public static final String KEY_USER = "user";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleName;

    /** 角色权限字符串 */
    private String roleKey;

    private String remark;

    /** 状态 1正常 0停用 */
    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
