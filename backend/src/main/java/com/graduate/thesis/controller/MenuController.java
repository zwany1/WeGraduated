package com.graduate.thesis.controller;

import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.common.Result;
import com.graduate.thesis.common.UserContext;
import com.graduate.thesis.dto.admin.MenuSaveDTO;
import com.graduate.thesis.dto.admin.MenuVO;
import com.graduate.thesis.service.MenuService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理接口
 */
@RestController
@RequestMapping("/admin/system/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** 菜单树(含按钮, 供管理端展示/编辑) */
    @GetMapping("/tree")
    @RequiresPerms("system:menu:list")
    public Result<List<MenuVO>> tree() {
        return Result.ok(menuService.listMenuTree());
    }

    /** 当前登录用户的可见菜单(供前端动态渲染侧边栏) */
    @GetMapping("/user-menus")
    public Result<List<MenuVO>> userMenus() {
        return Result.ok(menuService.listUserMenus(UserContext.get()));
    }

    @PostMapping
    @RequiresPerms("system:menu:add")
    public Result<Void> create(@RequestBody MenuSaveDTO dto) {
        menuService.createMenu(dto);
        return Result.ok();
    }

    @PutMapping
    @RequiresPerms("system:menu:edit")
    public Result<Void> update(@RequestBody MenuSaveDTO dto) {
        menuService.updateMenu(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPerms("system:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.ok();
    }
}
