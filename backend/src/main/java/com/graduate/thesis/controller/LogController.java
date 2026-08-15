package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.entity.LoginLog;
import com.graduate.thesis.service.LogService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审计日志接口
 */
@RestController
@RequestMapping("/admin/system/log")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/oper")
    @RequiresPerms("system:log:oper")
    public Result<PageResult<com.graduate.thesis.entity.OperLog>> operLogs(@RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "10") int size,
                                                                          @RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) Boolean status) {
        return Result.ok(logService.listOperLogs(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword, status));
    }

    @DeleteMapping("/oper")
    @OperLog(module = "操作日志", action = "删除操作日志")
    @RequiresPerms("system:log:oper")
    public Result<Void> deleteOper(@RequestBody List<Long> ids) {
        logService.deleteOperLogs(ids);
        return Result.ok();
    }

    @GetMapping("/login")
    @RequiresPerms("system:log:login")
    public Result<PageResult<LoginLog>> loginLogs(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean status) {
        return Result.ok(logService.listLoginLogs(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword, status));
    }

    @DeleteMapping("/login")
    @OperLog(module = "登录日志", action = "删除登录日志")
    @RequiresPerms("system:log:login")
    public Result<Void> deleteLogin(@RequestBody List<Long> ids) {
        logService.deleteLoginLogs(ids);
        return Result.ok();
    }
}
