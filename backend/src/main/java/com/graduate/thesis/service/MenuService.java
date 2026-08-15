package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.dto.admin.MenuSaveDTO;
import com.graduate.thesis.dto.admin.MenuVO;
import com.graduate.thesis.entity.Menu;
import com.graduate.thesis.entity.RoleMenu;
import com.graduate.thesis.mapper.MenuMapper;
import com.graduate.thesis.mapper.RoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单管理服务
 */
@Service
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final PermissionService permissionService;

    public MenuService(MenuMapper menuMapper,
                       RoleMenuMapper roleMenuMapper,
                       PermissionService permissionService) {
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.permissionService = permissionService;
    }

    public List<MenuVO> listMenuTree() {
        return permissionService.getAllMenuTree();
    }

    public List<MenuVO> listUserMenus(Long userId) {
        return permissionService.getUserMenuTree(userId);
    }

    @Transactional
    public void createMenu(MenuSaveDTO dto) {
        validate(dto);
        Menu menu = new Menu();
        apply(menu, dto);
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.insert(menu);
    }

    @Transactional
    public void updateMenu(MenuSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(400, "缺少菜单ID");
        }
        Menu menu = menuMapper.selectById(dto.getId());
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        // 不能把父节点设置为自己或自己的子节点
        if (dto.getParentId() != null && dto.getParentId().equals(dto.getId())) {
            throw new BusinessException(400, "父级菜单不能是自己");
        }
        apply(menu, dto);
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menu);
    }

    /** 删除菜单: 存在子菜单时禁止删除; 同时清理角色-菜单关联 */
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<Menu>()
                .eq(Menu::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException(400, "存在子菜单, 无法删除, 请先删除子菜单/子按钮");
        }
        menuMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, id));
    }

    private void validate(MenuSaveDTO dto) {
        if (!StringUtils.hasText(dto.getMenuName())) {
            throw new BusinessException(400, "菜单名称不能为空");
        }
        String type = dto.getMenuType() == null ? Menu.TYPE_MENU : dto.getMenuType();
        if (!Menu.TYPE_DIR.equals(type) && !Menu.TYPE_MENU.equals(type) && !Menu.TYPE_BUTTON.equals(type)) {
            throw new BusinessException(400, "非法菜单类型");
        }
        if ((Menu.TYPE_DIR.equals(type) || Menu.TYPE_MENU.equals(type)) && !StringUtils.hasText(dto.getPath())) {
            throw new BusinessException(400, "路由地址不能为空");
        }
        if (Menu.TYPE_BUTTON.equals(type) && !StringUtils.hasText(dto.getPerms())) {
            throw new BusinessException(400, "按钮权限标识不能为空");
        }
        if (dto.getParentId() == null) {
            dto.setParentId(0L);
        }
        if (dto.getOrderNum() == null) {
            dto.setOrderNum(0);
        }
        if (dto.getVisible() == null) {
            dto.setVisible(true);
        }
        if (dto.getStatus() == null) {
            dto.setStatus(true);
        }
    }

    private void apply(Menu menu, MenuSaveDTO dto) {
        menu.setParentId(dto.getParentId());
        menu.setMenuName(dto.getMenuName());
        menu.setMenuType(dto.getMenuType());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setPerms(dto.getPerms());
        menu.setIcon(dto.getIcon());
        menu.setOrderNum(dto.getOrderNum());
        menu.setVisible(dto.getVisible());
        menu.setStatus(dto.getStatus());
    }
}
