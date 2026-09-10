package com.graduate.thesis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduate.thesis.common.BusinessException;
import com.graduate.thesis.entity.Menu;
import com.graduate.thesis.entity.OperLog;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.RoleMenu;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.MenuMapper;
import com.graduate.thesis.mapper.OperLogMapper;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.RoleMenuMapper;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
import com.graduate.thesis.mapper.FormatTemplateMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作撤销: 可逆操作在服务层变更前把旧状态快照暂存于当前请求上下文,
 * 由日志切面在操作成功后写入日志行 undo_data 字段; 撤销时按类型重放快照.
 */
@Service
public class UndoService {

    private static final ThreadLocal<String> PENDING_SNAPSHOT = new ThreadLocal<>();

    private final OperLogMapper operLogMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final FormatTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<LoginSessionService> sessionServiceProvider;

    public UndoService(OperLogMapper operLogMapper,
                       UserMapper userMapper,
                       RoleMapper roleMapper,
                       MenuMapper menuMapper,
                       UserRoleMapper userRoleMapper,
                       RoleMenuMapper roleMenuMapper,
                       FormatTemplateMapper templateMapper,
                       ObjectMapper objectMapper,
                       ObjectProvider<LoginSessionService> sessionServiceProvider) {
        this.operLogMapper = operLogMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.templateMapper = templateMapper;
        this.objectMapper = objectMapper;
        this.sessionServiceProvider = sessionServiceProvider;
    }

    /** 服务层在变更前登记快照; 切面在操作结束后取走并落库, 未被消费的自动清理 */
    public static void stage(String json) {
        PENDING_SNAPSHOT.set(json);
    }

    public static String takeStaged() {
        String s = PENDING_SNAPSHOT.get();
        PENDING_SNAPSHOT.remove();
        return s;
    }

    // ==================== 快照采集(变更前调用) ====================

    public String snapshotUserStatus(List<Long> userIds) {
        Map<Long, Boolean> snapshot = new LinkedHashMap<>();
        for (Long id : userIds) {
            User u = userMapper.selectById(id);
            if (u != null) {
                snapshot.put(id, u.getStatus() == null || u.getStatus());
            }
        }
        return snapshot.isEmpty() ? null : toJson(Map.of("type", "userStatus", "status", snapshot));
    }

    public String snapshotUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
        return toJson(Map.of("type", "userRoles", "userId", userId, "roleIds", roleIds));
    }

    public String snapshotMarket(Long templateId) {
        com.graduate.thesis.entity.FormatTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("type", "market");
        m.put("templateId", templateId);
        m.put("isPublic", Boolean.TRUE.equals(t.getIsPublic()));
        m.put("recommended", Boolean.TRUE.equals(t.getRecommended()));
        m.put("category", t.getCategory());
        m.put("publicTime", t.getPublicTime() == null ? null : t.getPublicTime().toString());
        return toJson(m);
    }

    public String snapshotMenu(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("type", "menu");
        m.put("menu", menu);
        return toJson(m);
    }

    /** 删除菜单前的快照: 菜单行 + 各角色对该菜单的授权, 撤销时一并恢复 */
    public String snapshotMenuDelete(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);
        if (menu == null) {
            return null;
        }
        List<Long> roleIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getMenuId, menuId))
                .stream().map(RoleMenu::getRoleId).distinct().collect(Collectors.toList());
        Map<String, Object> m = new HashMap<>();
        m.put("type", "menuDelete");
        m.put("menu", menu);
        m.put("roleIds", roleIds);
        return toJson(m);
    }

    public String snapshotRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("type", "role");
        m.put("role", role);
        return toJson(m);
    }

    public String snapshotRoleMenus(Long roleId) {
        List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                        .eq(RoleMenu::getRoleId, roleId))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
        return toJson(Map.of("type", "roleMenus", "roleId", roleId, "menuIds", menuIds));
    }

    // ==================== 撤销重放 ====================

    /** 撤销日志 id 对应的操作, 恢复快照状态 */
    @Transactional
    public void undo(Long operLogId) {
        OperLog log = operLogMapper.selectById(operLogId);
        if (log == null || !Boolean.TRUE.equals(log.getStatus())) {
            throw new BusinessException(404, "日志不存在或操作未成功");
        }
        String undoData = log.getUndoData();
        if (undoData == null || undoData.isBlank()) {
            throw new BusinessException(400, "该操作不支持撤销");
        }
        Map<String, Object> snap = readSnapshot(undoData);
        String type = String.valueOf(snap.get("type"));
        if ("userStatus".equals(type)) {
            undoUserStatus(snap);
        } else if ("userRoles".equals(type)) {
            undoUserRoles(snap);
        } else if ("market".equals(type)) {
            undoMarket(snap);
        } else if ("menu".equals(type)) {
            undoMenu(snap);
        } else if ("menuDelete".equals(type)) {
            undoMenuDelete(snap);
        } else if ("role".equals(type)) {
            undoRole(snap);
        } else if ("roleMenus".equals(type)) {
            undoRoleMenus(snap);
        } else {
            throw new BusinessException(400, "未知快照类型: " + type);
        }
        operLogMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OperLog>()
                .eq(OperLog::getId, operLogId)
                .set(OperLog::getUndoData, null));
    }

    private void undoUserStatus(Map<String, Object> snap) {
        Map<Long, Boolean> status = readLongMap(snap.get("status"));
        for (Map.Entry<Long, Boolean> e : status.entrySet()) {
            User u = userMapper.selectById(e.getKey());
            if (u != null) {
                u.setStatus(e.getValue());
                userMapper.updateById(u);
            }
        }
    }

    private void undoUserRoles(Map<String, Object> snap) {
        Long userId = readLong(snap.get("userId"));
        List<Long> roleIds = readLongList(snap.get("roleIds"));
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            if (roleMapper.selectById(roleId) != null) {
                userRoleMapper.insert(new UserRole(userId, roleId));
            }
        }
        Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
        if (adminRole != null) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                u.setRole(roleIds.contains(adminRole.getId()) ? User.ROLE_ADMIN : User.ROLE_USER);
                userMapper.updateById(u);
            }
        }
        sessionServiceProvider.getObject().revokeAllForUser(userId);
    }

    private void undoMarket(Map<String, Object> snap) {
        Long templateId = readLong(snap.get("templateId"));
        com.graduate.thesis.entity.FormatTemplate t = templateMapper.selectById(templateId);
        if (t == null) {
            throw new BusinessException(404, "模板已不存在, 无法恢复");
        }
        t.setIsPublic(readBool(snap.get("isPublic")));
        t.setRecommended(readBool(snap.get("recommended")));
        Object category = snap.get("category");
        t.setCategory(category == null ? null : String.valueOf(category));
        Object publicTime = snap.get("publicTime");
        t.setPublicTime(publicTime == null ? null : java.time.LocalDateTime.parse(String.valueOf(publicTime)));
        templateMapper.updateById(t);
    }

    private void undoMenu(Map<String, Object> snap) {
        Object raw = snap.get("menu");
        Menu menu = objectMapper.convertValue(raw, Menu.class);
        Long parentId = menu.getParentId();
        if (parentId != null && parentId > 0 && menuMapper.selectById(parentId) == null) {
            throw new BusinessException(400, "原父级菜单已被删除, 无法恢复层级");
        }
        if (menuMapper.selectById(menu.getId()) == null) {
            menuMapper.insert(menu);
        } else {
            menuMapper.updateById(menu);
        }
    }

    private void undoMenuDelete(Map<String, Object> snap) {
        Object raw = snap.get("menu");
        Menu menu = objectMapper.convertValue(raw, Menu.class);
        Long parentId = menu.getParentId();
        if (parentId != null && parentId > 0 && menuMapper.selectById(parentId) == null) {
            throw new BusinessException(400, "原父级菜单已被删除, 请先撤销父级的删除");
        }
        if (menuMapper.selectById(menu.getId()) != null) {
            throw new BusinessException(400, "同 ID 菜单已存在, 无法恢复");
        }
        // 主键随快照带回, 恢复原菜单行(与种子数据同机制)
        menuMapper.insert(menu);
        List<Long> roleIds = readLongList(snap.get("roleIds"));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getMenuId, menu.getId()));
        for (Long roleId : roleIds) {
            if (roleMapper.selectById(roleId) != null) {
                roleMenuMapper.insert(new RoleMenu(roleId, menu.getId()));
            }
        }
    }

    private void undoRole(Map<String, Object> snap) {
        Object raw = snap.get("role");
        Role role = objectMapper.convertValue(raw, Role.class);
        if (roleMapper.selectById(role.getId()) == null) {
            roleMapper.insert(role);
        } else {
            roleMapper.updateById(role);
        }
    }

    private void undoRoleMenus(Map<String, Object> snap) {
        Long roleId = readLong(snap.get("roleId"));
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException(404, "角色已不存在, 无法恢复");
        }
        List<Long> menuIds = readLongList(snap.get("menuIds"));
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            if (menuMapper.selectById(menuId) != null) {
                roleMenuMapper.insert(new RoleMenu(roleId, menuId));
            }
        }
    }

    private String toJson(Map<String, Object> m) {
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException(400, "快照数据损坏, 无法撤销");
        }
    }

    private static Long readLong(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o));
    }

    private static Boolean readBool(Object o) {
        return o != null && Boolean.parseBoolean(String.valueOf(o));
    }

    private static List<Long> readLongList(Object o) {
        List<Long> result = new java.util.ArrayList<>();
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                result.add(Long.valueOf(String.valueOf(x)));
            }
        }
        return result;
    }

    private static Map<Long, Boolean> readLongMap(Object o) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        if (o instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                result.put(Long.valueOf(String.valueOf(e.getKey())), readBool(e.getValue()));
            }
        }
        return result;
    }
}
