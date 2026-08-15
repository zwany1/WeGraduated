package com.graduate.thesis.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.entity.Menu;
import com.graduate.thesis.entity.Role;
import com.graduate.thesis.entity.RoleMenu;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.entity.UserRole;
import com.graduate.thesis.mapper.MenuMapper;
import com.graduate.thesis.mapper.RoleMapper;
import com.graduate.thesis.mapper.RoleMenuMapper;
import com.graduate.thesis.mapper.UserMapper;
import com.graduate.thesis.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动迁移: 幂等补齐 t_user.role 列, 确保初始管理员账号存在;
 * 初始化 RBAC 权限体系(菜单/角色/用户角色/角色菜单), 兼容已有数据库。
 */
@Slf4j
@Component
public class DbMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final MenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final String adminUsername;
    private final String adminPassword;

    public DbMigrationRunner(JdbcTemplate jdbcTemplate,
                             UserMapper userMapper,
                             RoleMapper roleMapper,
                             UserRoleMapper userRoleMapper,
                             MenuMapper menuMapper,
                             RoleMenuMapper roleMenuMapper,
                             @Value("${thesis.admin.username:admin}") String adminUsername,
                             @Value("${thesis.admin.password:admin123}") String adminPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRoleColumn();
        ensureAdminAccount();
        ensureRoles();
        ensureMenus();
        ensureAdminMenus();
        syncUserRoles();
    }

    private void ensureRoleColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'role'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE t_user ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER'");
                log.info("[DbMigration] 已为 t_user 补齐 role 列");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 检查/补齐 role 列失败(可能库未就绪, 启动时会重试): {}", e.getMessage());
        }
    }

    private void ensureAdminAccount() {
        try {
            User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, adminUsername).last("LIMIT 1"));
            if (admin == null) {
                User user = new User();
                user.setUsername(adminUsername);
                user.setPassword(encoder.encode(adminPassword));
                user.setNickname("系统管理员");
                user.setRole(User.ROLE_ADMIN);
                user.setCreateTime(LocalDateTime.now());
                userMapper.insert(user);
                log.info("[DbMigration] 已创建初始管理员账号: username={}, 请尽快登录后修改密码", adminUsername);
            } else if (!User.ROLE_ADMIN.equals(admin.getRole())) {
                // 同名用户已存在但非管理员: 不自动提升, 防止抢注 admin 账号被提权, 由人工处理
                log.warn("[DbMigration] 已存在同名用户 [{}] 但非管理员, 跳过自动提升, 请人工核实处理", adminUsername);
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 初始化管理员失败: {}", e.getMessage());
        }
    }

    // ==================== RBAC 初始化 ====================

    private void ensureRoles() {
        try {
            ensureRole(Role.KEY_ADMIN, "超级管理员", "内置超级管理员, 拥有全部权限, 不可删除");
            ensureRole(Role.KEY_USER, "普通用户", "内置普通用户角色");
            log.info("[DbMigration] 角色初始化完成");
        } catch (Exception e) {
            log.warn("[DbMigration] 初始化角色失败: {}", e.getMessage());
        }
    }

    private void ensureRole(String roleKey, String roleName, String remark) {
        Role exists = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleKey, roleKey).last("LIMIT 1"));
        if (exists == null) {
            Role role = new Role();
            role.setRoleName(roleName);
            role.setRoleKey(roleKey);
            role.setRemark(remark);
            role.setStatus(true);
            role.setCreateTime(LocalDateTime.now());
            role.setUpdateTime(LocalDateTime.now());
            roleMapper.insert(role);
        }
    }

    /** 初始化管理后台菜单(仅当菜单表为空时) */
    private void ensureMenus() {
        try {
            Long count = menuMapper.selectCount(null);
            if (count != null && count > 0) {
                log.info("[DbMigration] 菜单已存在, 跳过初始化");
                return;
            }
            // 菜单树: id / parentId / 名称 / 类型 / path / component / perms / icon / 排序
            Map<String, Object[]> rows = new LinkedHashMap<>();
            rows.put("1000", new Object[]{0L, "运营概览", "C", "dashboard", "admin/Dashboard", "system:overview:view",
                    "chart", 1, "运营数据总览"});
            rows.put("1001", new Object[]{0L, "排版管理", "M", "format", null, null, "doc", 2, "排版相关管理"});
            rows.put("1002", new Object[]{1001L, "模板管理", "C", "templates", "admin/TemplateManage", "system:template:list",
                    "document", 1, "模板库管理"});
            rows.put("1003", new Object[]{1002L, "模板删除", "F", null, null, "system:template:delete", null, 1, "删除模板"});
            rows.put("1004", new Object[]{1001L, "排版任务", "C", "tasks", "admin/TaskManage", "system:task:list",
                    "task", 2, "排版任务管理"});
            rows.put("1005", new Object[]{0L, "系统管理", "M", "system", null, null, "setting", 3, "系统配置"});
            rows.put("1006", new Object[]{1005L, "用户管理", "C", "users", "admin/UserManage", "system:user:list",
                    "user", 1, "用户管理"});
            rows.put("1007", new Object[]{1006L, "用户查询", "F", null, null, "system:user:query", null, 1, "查询用户"});
            rows.put("1008", new Object[]{1006L, "用户新增", "F", null, null, "system:user:add", null, 2, "新增用户"});
            rows.put("1009", new Object[]{1006L, "用户编辑", "F", null, null, "system:user:edit", null, 3, "编辑用户"});
            rows.put("1010", new Object[]{1006L, "用户删除", "F", null, null, "system:user:delete", null, 4, "删除用户"});
            rows.put("1011", new Object[]{1006L, "分配角色", "F", null, null, "system:user:assign", null, 5, "分配角色"});
            rows.put("1012", new Object[]{1005L, "角色管理", "C", "role", "admin/RoleManage", "system:role:list",
                    "role", 2, "角色管理"});
            rows.put("1013", new Object[]{1012L, "角色新增", "F", null, null, "system:role:add", null, 1, "新增角色"});
            rows.put("1014", new Object[]{1012L, "角色编辑", "F", null, null, "system:role:edit", null, 2, "编辑角色"});
            rows.put("1015", new Object[]{1012L, "角色删除", "F", null, null, "system:role:delete", null, 3, "删除角色"});
            rows.put("1016", new Object[]{1012L, "分配菜单", "F", null, null, "system:role:assign", null, 4, "分配菜单"});
            rows.put("1017", new Object[]{1005L, "菜单管理", "C", "menu", "admin/MenuManage", "system:menu:list",
                    "menu", 3, "菜单管理"});
            rows.put("1018", new Object[]{1017L, "菜单新增", "F", null, null, "system:menu:add", null, 1, "新增菜单"});
            rows.put("1019", new Object[]{1017L, "菜单编辑", "F", null, null, "system:menu:edit", null, 2, "编辑菜单"});
            rows.put("1020", new Object[]{1017L, "菜单删除", "F", null, null, "system:menu:delete", null, 3, "删除菜单"});

            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, Object[]> e : rows.entrySet()) {
                Object[] v = e.getValue();
                Menu menu = new Menu();
                menu.setId(Long.valueOf(e.getKey()));
                menu.setParentId((Long) v[0]);
                menu.setMenuName((String) v[1]);
                menu.setMenuType((String) v[2]);
                menu.setPath((String) v[3]);
                menu.setComponent((String) v[4]);
                menu.setPerms((String) v[5]);
                menu.setIcon((String) v[6]);
                menu.setOrderNum((Integer) v[7]);
                menu.setVisible(true);
                menu.setStatus(true);
                menu.setCreateTime(now);
                menu.setUpdateTime(now);
                menuMapper.insert(menu);
            }
            log.info("[DbMigration] 已初始化管理后台菜单树");
        } catch (Exception e) {
            log.warn("[DbMigration] 初始化菜单失败: {}", e.getMessage());
        }
    }

    /** 给超管角色授予全部菜单(幂等) */
    private void ensureAdminMenus() {
        try {
            Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
            if (adminRole == null) {
                return;
            }
            Long granted = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenu>()
                    .eq(RoleMenu::getRoleId, adminRole.getId()));
            if (granted != null && granted > 0) {
                return;
            }
            List<Menu> menus = menuMapper.selectList(new LambdaQueryWrapper<Menu>()
                    .select(Menu::getId));
            for (Menu m : menus) {
                roleMenuMapper.insert(new RoleMenu(adminRole.getId(), m.getId()));
            }
            log.info("[DbMigration] 已为超管角色授予全部菜单({} 项)", menus.size());
        } catch (Exception e) {
            log.warn("[DbMigration] 授予超管菜单失败: {}", e.getMessage());
        }
    }

    /** 将存量 t_user.role 映射到 t_user_role(幂等: 仅处理未分配角色的用户) */
    private void syncUserRoles() {
        try {
            Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
            Role userRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleKey, Role.KEY_USER).last("LIMIT 1"));
            if (adminRole == null || userRole == null) {
                return;
            }
            List<User> users = userMapper.selectList(null);
            int synced = 0;
            for (User u : users) {
                Long assigned = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, u.getId()));
                if (assigned != null && assigned > 0) {
                    continue;
                }
                Long roleId = User.ROLE_ADMIN.equals(u.getRole()) ? adminRole.getId() : userRole.getId();
                userRoleMapper.insert(new UserRole(u.getId(), roleId));
                synced++;
            }
            if (synced > 0) {
                log.info("[DbMigration] 已为 {} 个存量用户同步角色", synced);
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 同步用户角色失败: {}", e.getMessage());
        }
    }
}
