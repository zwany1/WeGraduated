package com.graduate.thesis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单权限
 */
@Data
@TableName("t_menu")
public class Menu {

    /** 目录 */
    public static final String TYPE_DIR = "M";
    /** 菜单 */
    public static final String TYPE_MENU = "C";
    /** 按钮 */
    public static final String TYPE_BUTTON = "F";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单ID(顶层为 0) */
    private Long parentId;

    private String menuName;

    /** M目录 / C菜单 / F按钮 */
    private String menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识, 如 system:user:add */
    private String perms;

    private String icon;

    private Integer orderNum;

    /** 显示状态 1显示 0隐藏 */
    private Boolean visible;

    /** 状态 1正常 0停用 */
    private Boolean status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
