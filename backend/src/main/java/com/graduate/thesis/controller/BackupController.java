package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.service.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 数据库备份接口
 */
@RestController
@RequestMapping("/admin/system/backup")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /** 立即备份 */
    @PostMapping
    @OperLog(module = "数据备份", action = "立即备份")
    @RequiresPerms("system:backup:create")
    public Result<String> backup() {
        return Result.ok(backupService.backupNow());
    }

    /** 备份列表 */
    @GetMapping("/list")
    @RequiresPerms("system:backup:list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(backupService.listBackups());
    }

    /** 下载备份 */
    @GetMapping("/download/{name}")
    @RequiresPerms("system:backup:list")
    public ResponseEntity<FileSystemResource> download(@PathVariable String name) {
        File file = backupService.loadBackup(name);
        String encoded = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(new FileSystemResource(file));
    }

    /** 删除备份 */
    @DeleteMapping("/{name}")
    @OperLog(module = "数据备份", action = "删除备份")
    @RequiresPerms("system:backup:delete")
    public Result<Void> delete(@PathVariable String name) {
        backupService.deleteBackup(name);
        return Result.ok();
    }
}
