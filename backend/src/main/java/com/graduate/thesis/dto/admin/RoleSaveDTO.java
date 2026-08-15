package com.graduate.thesis.dto.admin;

import lombok.Data;

/**
 * 角色新增/修改
 */
@Data
public class RoleSaveDTO {

    private Long id;

    private String roleName;

    private String roleKey;

    private String remark;

    private Boolean status;
}
