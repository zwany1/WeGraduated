package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.dto.admin.AssignMenusDTO;
import com.graduate.thesis.dto.admin.RoleSaveDTO;
import com.graduate.thesis.dto.admin.RoleVO;
import com.graduate.thesis.service.RoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/admin/system/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/page")
    @RequiresPerms("system:role:list")
    public Result<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(roleService.listRoles(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @GetMapping("/all")
    @RequiresPerms({"system:role:list", "system:user:assign"})
    public Result<List<RoleVO>> all() {
        return Result.ok(roleService.listAllRoles());
    }

    @PostMapping
    @OperLog(module = "角色管理", action = "新增角色")
    @RequiresPerms("system:role:add")
    public Result<Void> create(@RequestBody RoleSaveDTO dto) {
        roleService.createRole(dto);
        return Result.ok();
    }

    @PutMapping
    @OperLog(module = "角色管理", action = "编辑角色")
    @RequiresPerms("system:role:edit")
    public Result<Void> update(@RequestBody RoleSaveDTO dto) {
        roleService.updateRole(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @OperLog(module = "角色管理", action = "删除角色")
    @RequiresPerms("system:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    /** 角色已分配的菜单ID */
    @GetMapping("/{id}/menus")
    @RequiresPerms("system:role:assign")
    public Result<List<Long>> roleMenus(@PathVariable Long id) {
        return Result.ok(roleService.getRoleMenuIds(id));
    }

    /** 给角色分配菜单 */
    @PutMapping("/assign-menus")
    @OperLog(module = "角色管理", action = "分配角色菜单")
    @RequiresPerms("system:role:assign")
    public Result<Void> assignMenus(@RequestBody AssignMenusDTO dto) {
        roleService.assignMenus(dto);
        return Result.ok();
    }
}
