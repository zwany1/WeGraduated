package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.admin.AdminStatsVO;
import com.graduate.thesis.dto.admin.AdminTaskVO;
import com.graduate.thesis.dto.admin.AdminTemplateVO;
import com.graduate.thesis.dto.admin.AdminUserVO;
import com.graduate.thesis.dto.admin.AssignRolesDTO;
import com.graduate.thesis.dto.admin.UserDetailVO;
import com.graduate.thesis.entity.FormatTask;
import com.graduate.thesis.service.AdminService;
import com.graduate.thesis.service.ExcelExportService;
import com.graduate.thesis.service.PaperService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final PaperService paperService;
    private final ExcelExportService excelExportService;

    public AdminController(AdminService adminService,
                           PaperService paperService,
                           ExcelExportService excelExportService) {
        this.adminService = adminService;
        this.paperService = paperService;
        this.excelExportService = excelExportService;
    }

    // ==================== 概览与统计 ====================

    @GetMapping("/stats/overview")
    @RequiresPerms("system:overview:view")
    public Result<AdminStatsVO> overview() {
        return Result.ok(adminService.overview());
    }

    @GetMapping("/stats/success-rate")
    @RequiresPerms("system:overview:view")
    public Result<Map<String, Object>> successRate() {
        return Result.ok(adminService.taskSuccessRate());
    }

    @GetMapping("/stats/failures")
    @RequiresPerms("system:overview:view")
    public Result<List<Map<String, Object>>> failures() {
        return Result.ok(adminService.failureReasons());
    }

    @GetMapping("/stats/top-templates")
    @RequiresPerms("system:overview:view")
    public Result<List<Map<String, Object>>> topTemplates() {
        return Result.ok(adminService.topTemplates());
    }

    @GetMapping("/stats/top-users")
    @RequiresPerms("system:overview:view")
    public Result<List<Map<String, Object>>> topUsers() {
        return Result.ok(adminService.topUsers());
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    @RequiresPerms("system:user:list")
    public Result<PageResult<AdminUserVO>> users(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listUsers(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @PutMapping("/users/{id}/role")
    @OperLog(module = "用户管理", action = "修改用户角色")
    @RequiresPerms("system:user:edit")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.updateUserRole(id, body.get("role"), UserContext.get());
        return Result.ok();
    }

    @PutMapping("/users/{id}/roles")
    @OperLog(module = "用户管理", action = "分配用户角色")
    @RequiresPerms("system:user:assign")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody AssignRolesDTO dto) {
        dto.setUserId(id);
        adminService.assignUserRoles(dto.getUserId(), dto.getRoleIds(), UserContext.get());
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    @OperLog(module = "用户管理", action = "封禁/启用用户")
    @RequiresPerms("system:user:status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminService.updateUserStatus(id, body.get("status"), UserContext.get());
        return Result.ok();
    }

    @PutMapping("/users/{id}/password")
    @OperLog(module = "用户管理", action = "重置用户密码")
    @RequiresPerms("system:user:resetPwd")
    public Result<Void> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        adminService.resetUserPassword(id, body.get("password"));
        return Result.ok();
    }

    @GetMapping("/users/{id}/detail")
    @RequiresPerms("system:user:list")
    public Result<UserDetailVO> userDetail(@PathVariable Long id) {
        return Result.ok(adminService.getUserDetail(id));
    }

    @DeleteMapping("/users/{id}")
    @OperLog(module = "用户管理", action = "删除用户")
    @RequiresPerms("system:user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id, UserContext.get());
        return Result.ok();
    }

    // ==================== 模板管理 ====================

    @GetMapping("/templates")
    @RequiresPerms("system:template:list")
    public Result<PageResult<AdminTemplateVO>> templates(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listTemplates(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @DeleteMapping("/templates/{id}")
    @OperLog(module = "模板管理", action = "删除模板")
    @RequiresPerms("system:template:delete")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        adminService.deleteTemplate(id);
        return Result.ok();
    }

    // ==================== 模板市场 ====================

    @GetMapping("/market/templates")
    @RequiresPerms("system:market:list")
    public Result<PageResult<Map<String, Object>>> marketTemplates(@RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "10") int size,
                                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listMarketTemplates(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @GetMapping("/market/templates/{id}/detail")
    @RequiresPerms("system:market:list")
    public Result<Map<String, Object>> marketTemplateDetail(@PathVariable Long id) {
        return Result.ok(adminService.marketTemplateDetail(id));
    }

    @PutMapping("/market/templates/{id}")
    @OperLog(module = "模板市场", action = "上架/推荐模板")
    @RequiresPerms("system:market:edit")
    public Result<Void> setMarket(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean isPublic = body.get("isPublic") == null ? null
                : Boolean.valueOf(String.valueOf(body.get("isPublic")));
        Boolean recommended = body.get("recommended") == null ? null
                : Boolean.valueOf(String.valueOf(body.get("recommended")));
        adminService.setMarketTemplate(id, isPublic, recommended);
        return Result.ok();
    }

    // ==================== 任务管理 ====================

    @GetMapping("/tasks")
    @RequiresPerms("system:task:list")
    public Result<PageResult<AdminTaskVO>> tasks(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listTasks(Math.max(page, 1), Math.min(Math.max(size, 1), 100), status, keyword));
    }

    @GetMapping("/tasks/{id}/detail")
    @RequiresPerms("system:task:list")
    public Result<FormatTask> taskDetail(@PathVariable Long id) {
        return Result.ok(paperService.getTaskDetail(id));
    }

    @PostMapping("/tasks/{id}/rerun")
    @OperLog(module = "排版任务", action = "任务重跑")
    @RequiresPerms("system:task:rerun")
    public Result<FormatTask> rerunTask(@PathVariable Long id) {
        return Result.ok(paperService.rerun(id));
    }

    @PostMapping("/tasks/{id}/cancel")
    @OperLog(module = "排版任务", action = "任务取消")
    @RequiresPerms("system:task:cancel")
    public Result<Void> cancelTask(@PathVariable Long id) {
        paperService.cancel(id);
        return Result.ok();
    }

    // ==================== 报表导出 ====================

    @GetMapping("/export/users")
    @RequiresPerms("system:user:export")
    public ResponseEntity<byte[]> exportUsers() {
        List<AdminUserVO> users = adminService.listAllUsersForExport();
        String[] headers = {"ID", "用户名", "邮箱", "昵称", "角色", "状态", "模板数", "任务数", "文件数", "创建时间"};
        List<String[]> rows = new ArrayList<>();
        for (AdminUserVO u : users) {
            rows.add(new String[]{String.valueOf(u.getId()), u.getUsername(), safe(u.getEmail()),
                    safe(u.getNickname()), safe(u.getRole()), Boolean.FALSE.equals(u.getStatus()) ? "禁用" : "正常",
                    String.valueOf(u.getTemplateCount()), String.valueOf(u.getTaskCount()),
                    String.valueOf(u.getPaperCount()), fmt(u.getCreateTime())});
        }
        return excel(exportFileName("用户报表"), "用户数据", headers, rows);
    }

    @GetMapping("/export/templates")
    @RequiresPerms("system:template:export")
    public ResponseEntity<byte[]> exportTemplates() {
        List<AdminTemplateVO> list = adminService.listAllTemplatesForExport();
        String[] headers = {"ID", "模板名称", "所属用户", "规则数", "被引用任务数", "生成目录", "创建时间", "更新时间"};
        List<String[]> rows = new ArrayList<>();
        for (AdminTemplateVO t : list) {
            rows.add(new String[]{String.valueOf(t.getId()), safe(t.getName()), safe(t.getUsername()),
                    String.valueOf(t.getRuleCount()), String.valueOf(t.getTaskCount()),
                    Boolean.TRUE.equals(t.getGenerateToc()) ? "是" : "否", fmt(t.getCreateTime()), fmt(t.getUpdateTime())});
        }
        return excel(exportFileName("模板报表"), "模板数据", headers, rows);
    }

    @GetMapping("/export/tasks")
    @RequiresPerms("system:task:export")
    public ResponseEntity<byte[]> exportTasks() {
        List<AdminTaskVO> list = adminService.listAllTasksForExport();
        String[] headers = {"ID", "用户", "模板", "论文文件", "状态", "进度", "错误信息", "创建时间", "完成时间"};
        List<String[]> rows = new ArrayList<>();
        for (AdminTaskVO t : list) {
            rows.add(new String[]{String.valueOf(t.getId()), safe(t.getUsername()), safe(t.getTemplateName()),
                    safe(t.getOriginalName()), safe(t.getStatus()), String.valueOf(t.getProgress()),
                    safe(t.getErrorMsg()), fmt(t.getCreateTime()), fmt(t.getFinishTime())});
        }
        return excel(exportFileName("任务报表"), "任务数据", headers, rows);
    }

    private ResponseEntity<byte[]> excel(String fileName, String sheet, String[] headers, List<String[]> rows) {
        byte[] data = excelExportService.export(sheet, headers, rows);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private String exportFileName(String name) {
        return name + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
