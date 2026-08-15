package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 给用户分配角色
 */
@Data
public class AssignRolesDTO {

    private Long userId;

    private List<Long> roleIds;
}
