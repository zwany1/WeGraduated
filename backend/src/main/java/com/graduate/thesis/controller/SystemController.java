package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.OperLog;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.PageResult;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.entity.Config;
import com.graduate.thesis.entity.DictData;
import com.graduate.thesis.entity.DictType;
import com.graduate.thesis.entity.Notice;
import com.graduate.thesis.service.SystemService;
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
 * 系统配置接口: 字典 / 参数 / 公告
 */
@RestController
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    // ==================== 字典类型 ====================

    @GetMapping("/admin/system/dict/type/page")
    @RequiresPerms("system:dict:list")
    public Result<PageResult<DictType>> dictTypePage(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String keyword) {
        return Result.ok(systemService.listDictTypes(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @GetMapping("/admin/system/dict/type/all")
    @RequiresPerms("system:dict:list")
    public Result<List<DictType>> dictTypeAll() {
        return Result.ok(systemService.listAllDictTypes());
    }

    @PostMapping("/admin/system/dict/type")
    @OperLog(module = "字典管理", action = "新增字典类型")
    @RequiresPerms("system:dict:add")
    public Result<Void> saveDictType(@RequestBody DictType dto) {
        systemService.saveDictType(dto);
        return Result.ok();
    }

    @PutMapping("/admin/system/dict/type")
    @OperLog(module = "字典管理", action = "编辑字典类型")
    @RequiresPerms("system:dict:edit")
    public Result<Void> updateDictType(@RequestBody DictType dto) {
        systemService.saveDictType(dto);
        return Result.ok();
    }

    @DeleteMapping("/admin/system/dict/type/{id}")
    @OperLog(module = "字典管理", action = "删除字典类型")
    @RequiresPerms("system:dict:delete")
    public Result<Void> deleteDictType(@PathVariable Long id) {
        systemService.deleteDictType(id);
        return Result.ok();
    }

    // ==================== 字典数据 ====================

    @GetMapping("/admin/system/dict/data")
    @RequiresPerms("system:dict:list")
    public Result<List<DictData>> dictData(@RequestParam String dictType) {
        return Result.ok(systemService.listDictData(dictType));
    }

    @GetMapping("/admin/system/dict/data/page")
    @RequiresPerms("system:dict:list")
    public Result<PageResult<DictData>> dictDataPage(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String dictType) {
        return Result.ok(systemService.pageDictData(Math.max(page, 1), Math.min(Math.max(size, 1), 100), dictType));
    }

    @PostMapping("/admin/system/dict/data")
    @OperLog(module = "字典管理", action = "新增字典数据")
    @RequiresPerms("system:dict:add")
    public Result<Void> saveDictData(@RequestBody DictData dto) {
        systemService.saveDictData(dto);
        return Result.ok();
    }

    @PutMapping("/admin/system/dict/data")
    @OperLog(module = "字典管理", action = "编辑字典数据")
    @RequiresPerms("system:dict:edit")
    public Result<Void> updateDictData(@RequestBody DictData dto) {
        systemService.saveDictData(dto);
        return Result.ok();
    }

    @DeleteMapping("/admin/system/dict/data/{id}")
    @OperLog(module = "字典管理", action = "删除字典数据")
    @RequiresPerms("system:dict:delete")
    public Result<Void> deleteDictData(@PathVariable Long id) {
        systemService.deleteDictData(id);
        return Result.ok();
    }

    // ==================== 系统参数 ====================

    @GetMapping("/admin/system/config/page")
    @RequiresPerms("system:config:list")
    public Result<PageResult<Config>> configPage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(systemService.listConfigs(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @PostMapping("/admin/system/config")
    @OperLog(module = "参数设置", action = "新增参数")
    @RequiresPerms("system:config:add")
    public Result<Void> saveConfig(@RequestBody Config dto) {
        systemService.saveConfig(dto);
        return Result.ok();
    }

    @PutMapping("/admin/system/config")
    @OperLog(module = "参数设置", action = "编辑参数")
    @RequiresPerms("system:config:edit")
    public Result<Void> updateConfig(@RequestBody Config dto) {
        systemService.saveConfig(dto);
        return Result.ok();
    }

    @DeleteMapping("/admin/system/config/{id}")
    @OperLog(module = "参数设置", action = "删除参数")
    @RequiresPerms("system:config:delete")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        systemService.deleteConfig(id);
        return Result.ok();
    }

    // ==================== 通知公告 ====================

    @GetMapping("/admin/system/notice/page")
    @RequiresPerms("system:notice:list")
    public Result<PageResult<Notice>> noticePage(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(systemService.listNotices(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @PostMapping("/admin/system/notice")
    @OperLog(module = "公告管理", action = "新增公告")
    @RequiresPerms("system:notice:add")
    public Result<Void> saveNotice(@RequestBody Notice dto) {
        systemService.saveNotice(dto, UserContext.get());
        return Result.ok();
    }

    @PutMapping("/admin/system/notice")
    @OperLog(module = "公告管理", action = "编辑公告")
    @RequiresPerms("system:notice:edit")
    public Result<Void> updateNotice(@RequestBody Notice dto) {
        systemService.saveNotice(dto, UserContext.get());
        return Result.ok();
    }

    @DeleteMapping("/admin/system/notice/{id}")
    @OperLog(module = "公告管理", action = "删除公告")
    @RequiresPerms("system:notice:delete")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        systemService.deleteNotice(id);
        return Result.ok();
    }

    // ==================== 前台: 公开公告 ====================

    @GetMapping("/public/notice/list")
    public Result<List<Notice>> publicNotices(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(systemService.listPublicNotices(limit));
    }
}
