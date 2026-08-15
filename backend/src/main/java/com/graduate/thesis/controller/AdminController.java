package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.admin.AdminStatsVO;
import com.graduate.thesis.dto.admin.AdminTaskVO;
import com.graduate.thesis.dto.admin.AdminTemplateVO;
import com.graduate.thesis.dto.admin.AdminUserVO;
import com.graduate.thesis.dto.admin.AssignRolesDTO;
import com.graduate.thesis.service.AdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理后台接口(仅 ADMIN 可访问, 由 AdminInterceptor 校验)
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 概览统计 */
    @GetMapping("/stats/overview")
    @RequiresPerms("system:overview:view")
    public Result<AdminStatsVO> overview() {
        return Result.ok(adminService.overview());
    }

    /** 用户分页 */
    @GetMapping("/users")
    @RequiresPerms("system:user:list")
    public Result<PageResult<AdminUserVO>> users(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listUsers(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    /** 修改用户角色 */
    @PutMapping("/users/{id}/role")
    @RequiresPerms("system:user:edit")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updateUserRole(id, body.get("role"), UserContext.get());
        return Result.ok();
    }

    /** 给用户分配角色(全量覆盖) */
    @PutMapping("/users/{id}/roles")
    @RequiresPerms("system:user:assign")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        dto.setUserId(id);
        adminService.assignUserRoles(dto.getUserId(), dto.getRoleIds(), UserContext.get());
        return Result.ok();
    }

    /** 删除用户(级联删除其全部数据) */
    @DeleteMapping("/users/{id}")
    @RequiresPerms("system:user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id, UserContext.get());
        return Result.ok();
    }

    /** 模板分页 */
    @GetMapping("/templates")
    @RequiresPerms("system:template:list")
    public Result<PageResult<AdminTemplateVO>> templates(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listTemplates(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    /** 删除模板(连同其格式规则) */
    @DeleteMapping("/templates/{id}")
    @RequiresPerms("system:template:delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        adminService.deleteTemplate(id);
        return Result.ok();
    }

    /** 任务分页 */
    @GetMapping("/tasks")
    @RequiresPerms("system:task:list")
    public Result<PageResult<AdminTaskVO>> tasks(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listTasks(Math.max(page, 1), Math.min(Math.max(size, 1), 100), status, keyword));
    }
}
