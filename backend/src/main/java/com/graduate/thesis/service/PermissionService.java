package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.annotation.RequiresPerms;
import com.graduate.thesis.dto.admin.MenuVO;
import com.graduate.thesis.entity.Menu;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.RoleMenu;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.MenuMapper;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.RoleMenuMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 权限服务: 用户角色 / 权限标识 / 动态菜单
 */
@Service
public class PermissionService {

    /** 超管权限通配 */
    public static final String ALL_PERMISSION = "*:*:*";

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    public PermissionService(UserRoleMapper userRoleMapper,
                             RoleMapper roleMapper,
                             RoleMenuMapper roleMenuMapper,
                             MenuMapper menuMapper) {
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
    }

    // ==================== 角色 ====================

    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    public List<Role> getUserRoles(Long userId) {
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectBatchIds(roleIds);
    }

    public Set<String> getUserRoleKeys(Long userId) {
        return getUserRoles(userId).stream()
                .filter(r -> r.getStatus() == null || r.getStatus())
                .map(Role::getRoleKey)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(Long userId, String roleKey) {
        return getUserRoleKeys(userId).contains(roleKey);
    }

    public boolean isAdmin(Long userId) {
        return hasRole(userId, Role.KEY_ADMIN);
    }

    // ==================== 权限标识 ====================

    /**
     * 用户拥有的权限标识集合. 超管返回包含 ALL_PERMISSION("*:*:*") 的集合.
     */
    public Set<String> getUserPerms(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 超管: 拥有全部权限
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.stream().anyMatch(r -> Role.KEY_ADMIN.equals(r.getRoleKey()))) {
            Set<String> all = new HashSet<>();
            all.add(ALL_PERMISSION);
            return all;
        }
        List<Long> enabledRoleIds = roles.stream()
                .filter(r -> r.getStatus() == null || r.getStatus())
                .map(Role::getId).collect(Collectors.toList());
        if (enabledRoleIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .in(RoleMenu::getRoleId, enabledRoleIds))
                .stream().map(RoleMenu::getMenuId).distinct().collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(m -> StringUtils.hasText(m.getPerms()))
                .filter(m -> m.getStatus() == null || m.getStatus())
                .map(Menu::getPerms)
                .collect(Collectors.toSet());
    }

    public boolean hasPerm(Long userId, String perm) {
        if (!StringUtils.hasText(perm)) {
            return true;
        }
        Set<String> perms = getUserPerms(userId);
        if (perms.contains(ALL_PERMISSION)) {
            return true;
        }
        return perms.contains(perm);
    }

    public boolean hasPerms(Long userId, String[] perms, RequiresPerms.Logical logical) {
        if (perms == null || perms.length == 0) {
            return true;
        }
        Set<String> owned = getUserPerms(userId);
        if (owned.contains(ALL_PERMISSION)) {
            return true;
        }
        if (logical == RequiresPerms.Logical.ALL) {
            for (String p : perms) {
                if (!owned.contains(p)) {
                    return false;
                }
            }
            return true;
        }
        for (String p : perms) {
            if (owned.contains(p)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 动态菜单 ====================

    /**
     * 用户可见的菜单树(目录+菜单, 不含按钮), 按 orderNum 排序.
     * 超管返回全部.
     */
    public List<MenuVO> getUserMenuTree(Long userId) {
        List<Menu> menus;
        if (isAdmin(userId)) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                    .ne(Menu::getMenuType, Menu.TYPE_BUTTON)
                    .eq(Menu::getStatus, true)
                    .orderByAsc(Menu::getOrderNum));
        } else {
            List<Long> roleIds = getUserRoleIds(userId);
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Role> roles = roleMapper.selectBatchIds(roleIds);
            List<Long> enabledRoleIds = roles.stream()
                    .filter(r -> r.getStatus() == null || r.getStatus())
                    .map(Role::getId).collect(Collectors.toList());
            if (enabledRoleIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                            .in(RoleMenu::getRoleId, enabledRoleIds))
                    .stream().map(RoleMenu::getMenuId).distinct().collect(Collectors.toList());
            if (menuIds.isEmpty()) {
                return Collections.emptyList();
            }
            menus = menuMapper.selectBatchIds(menuIds).stream()
                    .filter(m -> !Menu.TYPE_BUTTON.equals(m.getMenuType()))
                    .filter(m -> m.getStatus() == null || m.getStatus())
                    .filter(m -> m.getVisible() == null || m.getVisible())
                    .sorted(Comparator.comparing(Menu::getOrderNum,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
        }
        return buildTree(menus);
    }

    /** 全部菜单树(管理端用), 含目录/菜单/按钮 */
    public List<MenuVO> getAllMenuTree() {
        List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getOrderNum));
        return buildTree(menus);
    }

    private List<MenuVO> buildTree(List<Menu> menus) {
        if (menus.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MenuVO> nodeMap = new LinkedHashMap<>();
        for (Menu m : menus) {
            MenuVO vo = toVO(m);
            nodeMap.put(m.getId(), vo);
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO vo : nodeMap.values()) {
            MenuVO parent = nodeMap.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    private MenuVO toVO(Menu m) {
        MenuVO vo = new MenuVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setMenuName(m.getMenuName());
        vo.setMenuType(m.getMenuType());
        vo.setPath(m.getPath());
        vo.setComponent(m.getComponent());
        vo.setPerms(m.getPerms());
        vo.setIcon(m.getIcon());
        vo.setOrderNum(m.getOrderNum());
        vo.setVisible(m.getVisible());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
