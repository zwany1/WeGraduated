package com.graduate.thesis.dto.admin;

import lombok.Data;

/**
 * 菜单新增/修改
 */
@Data
public class MenuSaveDTO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer orderNum;

    private Boolean visible;

    private Boolean status;
}
