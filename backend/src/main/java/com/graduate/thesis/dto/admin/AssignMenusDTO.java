package com.graduate.thesis.dto.admin;

import lombok.Data;

import java.util.List;

/**
 * 给角色分配菜单
 */
@Data
public class AssignMenusDTO {

    private Long roleId;

    private List<Long> menuIds;
}
