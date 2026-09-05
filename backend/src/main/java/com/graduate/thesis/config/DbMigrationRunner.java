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
                             @Value("${thesis.admin.password}") String adminPassword) {
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
        ensureExtColumns();
        ensureAdminAccount();
        ensureRoles();
        ensureMenus();
        ensureAdminMenus();
        syncUserRoles();
        seedDictsAndConfigs();
    }

    /** 幂等补齐扩展列: t_user.status / 模板市场字段(兼容 MySQL 5.7 / 8.0) */
    private void ensureExtColumns() {
        addColumnIfMissing("t_user", "status", "ALTER TABLE t_user ADD COLUMN status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用'");
        // 个人资料扩展(与 deploy/migrations/2026-09-04-user-profile-ext.sql 对齐): 本地开发无 update.sh, 由启动迁移补列
        addColumnIfMissing("t_user", "bio", "ALTER TABLE t_user ADD COLUMN bio VARCHAR(200) DEFAULT NULL COMMENT '个人简介'");
        addColumnIfMissing("t_user", "gender", "ALTER TABLE t_user ADD COLUMN gender TINYINT DEFAULT NULL COMMENT '性别 0不填 1男 2女'");
        addColumnIfMissing("t_user", "school", "ALTER TABLE t_user ADD COLUMN school VARCHAR(80) DEFAULT NULL COMMENT '学校/单位'");
        addColumnIfMissing("t_user", "major", "ALTER TABLE t_user ADD COLUMN major VARCHAR(80) DEFAULT NULL COMMENT '学院/专业'");
        addColumnIfMissing("t_user", "city", "ALTER TABLE t_user ADD COLUMN city VARCHAR(40) DEFAULT NULL COMMENT '常驻城市'");
        addColumnIfMissing("t_user", "phone", "ALTER TABLE t_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话'");
        addColumnIfMissing("t_format_template", "is_public", "ALTER TABLE t_format_template ADD COLUMN is_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否上架模板市场'");
        addColumnIfMissing("t_format_template", "recommended", "ALTER TABLE t_format_template ADD COLUMN recommended TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐'");
        addColumnIfMissing("t_format_template", "public_time", "ALTER TABLE t_format_template ADD COLUMN public_time DATETIME DEFAULT NULL COMMENT '上架时间'");
        addColumnIfMissing("t_format_template", "category", "ALTER TABLE t_format_template ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '市场分类'");
        addColumnIfMissing("t_format_template", "source_template_id", "ALTER TABLE t_format_template ADD COLUMN source_template_id BIGINT DEFAULT NULL COMMENT '复制自市场模板的源模板'");
        addColumnIfMissing("t_format_template", "generate_abstract", "ALTER TABLE t_format_template ADD COLUMN generate_abstract TINYINT(1) DEFAULT 0 COMMENT '是否排版摘要区段'");
        addColumnIfMissing("t_format_template", "toc_config", "ALTER TABLE t_format_template ADD COLUMN toc_config TEXT DEFAULT NULL COMMENT '目录样式配置JSON'");
        addColumnIfMissing("t_format_template", "rating_avg", "ALTER TABLE t_format_template ADD COLUMN rating_avg DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '平均评分'");
        addColumnIfMissing("t_format_template", "rating_count", "ALTER TABLE t_format_template ADD COLUMN rating_count INT NOT NULL DEFAULT 0 COMMENT '评分人数'");
        addColumnIfMissing("t_format_task", "retry_count", "ALTER TABLE t_format_task ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '失败自动重试次数'");
        addColumnIfMissing("t_format_task", "summary", "ALTER TABLE t_format_task ADD COLUMN summary VARCHAR(500) DEFAULT NULL COMMENT '排版校验摘要'");
        addColumnIfMissing("t_format_task", "report", "ALTER TABLE t_format_task ADD COLUMN report MEDIUMTEXT DEFAULT NULL COMMENT '排版体检报告JSON'");
        addColumnIfMissing("t_format_task", "heading_overrides", "ALTER TABLE t_format_task ADD COLUMN heading_overrides TEXT DEFAULT NULL COMMENT '标题覆盖JSON(引导修复重排)'");
        addColumnIfMissing("t_format_rule", "caption_enabled", "ALTER TABLE t_format_rule ADD COLUMN caption_enabled TINYINT(1) DEFAULT NULL COMMENT '标题编号是否启用'");
        addColumnIfMissing("t_format_template", "team_id", "ALTER TABLE t_format_template ADD COLUMN team_id BIGINT DEFAULT NULL COMMENT '所属团队(空=个人)'");
        addColumnIfMissing("t_format_task", "team_id", "ALTER TABLE t_format_task ADD COLUMN team_id BIGINT DEFAULT NULL COMMENT '所属团队(空=个人)'");
        addColumnIfMissing("t_paper_file", "team_id", "ALTER TABLE t_paper_file ADD COLUMN team_id BIGINT DEFAULT NULL COMMENT '所属团队(空=个人)'");
        ensureMarketRatingTable();
        ensureMarketCommentTable();
        ensureSiteCaseTable();
        addColumnIfMissing("t_site_case", "metrics", "ALTER TABLE t_site_case ADD COLUMN metrics VARCHAR(500) DEFAULT NULL COMMENT '成果指标JSON'");
        addColumnIfMissing("t_site_case", "detail", "ALTER TABLE t_site_case ADD COLUMN detail TEXT COMMENT '详情正文'");
        addColumnIfMissing("t_site_case", "images", "ALTER TABLE t_site_case ADD COLUMN images LONGTEXT COMMENT '截图base64JSON数组'");
        addColumnIfMissing("t_site_case", "template_id", "ALTER TABLE t_site_case ADD COLUMN template_id BIGINT DEFAULT NULL COMMENT '关联模板'");
        addColumnIfMissing("t_site_case", "task_id", "ALTER TABLE t_site_case ADD COLUMN task_id BIGINT DEFAULT NULL COMMENT '关联排版任务(空=手写示范)'");
        ensureMarketLikeTable();
        ensureLoginSessionTable();
        ensureFavoriteTable();
        ensureTeamTables();
        ensureNotificationTables();
        ensureFeedbackTable();
        addColumnIfMissing("t_feedback", "images",
                "ALTER TABLE t_feedback ADD COLUMN images LONGTEXT DEFAULT NULL COMMENT '图片base64 JSON数组'");
    }

    private void addColumnIfMissing(String table, String column, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (count == null || count == 0) {
                jdbcTemplate.execute(alterSql);
                log.info("[DbMigration] 已为 {}.{} 补齐列", table, column);
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 补齐 {}.{} 列失败: {}", table, column, e.getMessage());
        }
    }

    /** 模板市场评分表(幂等建表): 每个用户对每个市场模板最多一条评分 */
    private void ensureMarketRatingTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_market_rating'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_market_rating (" +
                        "user_id BIGINT NOT NULL, " +
                        "template_id BIGINT NOT NULL, " +
                        "score TINYINT NOT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (user_id, template_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板市场评分'");
                log.info("[DbMigration] 已创建 t_market_rating 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_market_rating 表失败: {}", e.getMessage());
        }
    }

    /** 模板市场评论表(幂等建表) */
    private void ensureMarketCommentTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_market_comment'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_market_comment (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "template_id BIGINT NOT NULL, " +
                        "user_id BIGINT NOT NULL, " +
                        "content VARCHAR(500) NOT NULL, " +
                        "parent_id BIGINT DEFAULT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (id), " +
                        "KEY idx_template (template_id), " +
                        "KEY idx_user (user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板市场评论'");
                log.info("[DbMigration] 已创建 t_market_comment 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_market_comment 表失败: {}", e.getMessage());
        }
    }

    /** 项目案例表(幂等建表) */
    private void ensureSiteCaseTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_site_case'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_site_case (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "tag VARCHAR(32) DEFAULT NULL, " +
                        "title VARCHAR(128) NOT NULL, " +
                        "description VARCHAR(500) DEFAULT NULL, " +
                        "color VARCHAR(16) DEFAULT 'blue', " +
                        "author VARCHAR(64) DEFAULT NULL, " +
                        "school VARCHAR(128) DEFAULT NULL, " +
                        "rating DECIMAL(2,1) DEFAULT 0.0, " +
                        "sort_order INT DEFAULT 0, " +
                        "visible TINYINT(1) DEFAULT 1, " +
                        "metrics VARCHAR(500) DEFAULT NULL, " +
                        "detail TEXT, " +
                        "images LONGTEXT, " +
                        "template_id BIGINT DEFAULT NULL, " +
                        "task_id BIGINT DEFAULT NULL, " +
                        "create_user BIGINT DEFAULT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (id), " +
                        "KEY idx_sort (sort_order)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目案例'");
                log.info("[DbMigration] 已创建 t_site_case 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_site_case 表失败: {}", e.getMessage());
        }
    }

    /** 模板市场点赞表(幂等建表): 每用户每模板最多一次点赞 */
    private void ensureMarketLikeTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_market_like'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_market_like (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "template_id BIGINT NOT NULL, " +
                        "user_id BIGINT NOT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (id), " +
                        "UNIQUE KEY uk_template_user (template_id, user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板市场点赞'");
                log.info("[DbMigration] 已创建 t_market_like 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_market_like 表失败: {}", e.getMessage());
        }
    }

    /** 登录会话表(幂等建表): 记录活跃登录, 支持后台查看在线用户与强制下线 */
    private void ensureLoginSessionTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_login_session'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_login_session (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id BIGINT NOT NULL, " +
                        "username VARCHAR(64) DEFAULT NULL, " +
                        "token VARCHAR(600) NOT NULL, " +
                        "ip VARCHAR(64) DEFAULT NULL, " +
                        "login_time DATETIME DEFAULT NULL, " +
                        "expire_time BIGINT NOT NULL, " +
                        "KEY idx_login_user (user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话'");
                log.info("[DbMigration] 已创建 t_login_session 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_login_session 表失败: {}", e.getMessage());
        }
    }

    /** 模板收藏表(幂等建表): 用户对市场模板的收藏 */
    private void ensureFavoriteTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_template_favorite'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_template_favorite (" +
                        "user_id BIGINT NOT NULL, " +
                        "template_id BIGINT NOT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "PRIMARY KEY (user_id, template_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板收藏'");
                log.info("[DbMigration] 已创建 t_template_favorite 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_template_favorite 表失败: {}", e.getMessage());
        }
    }

    /** 团队表(幂等建表): 多人协作, 团队内共享模板/论文/任务 */
    private void ensureTeamTables() {
        try {
            Integer c1 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_team'",
                    Integer.class);
            if (c1 == null || c1 == 0) {
                jdbcTemplate.execute("CREATE TABLE t_team (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(64) NOT NULL, " +
                        "description VARCHAR(255) DEFAULT NULL, " +
                        "owner_id BIGINT NOT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队'");
                log.info("[DbMigration] 已创建 t_team 表");
            }
            Integer c2 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_team_member'",
                    Integer.class);
            if (c2 == null || c2 == 0) {
                jdbcTemplate.execute("CREATE TABLE t_team_member (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "team_id BIGINT NOT NULL, " +
                        "user_id BIGINT NOT NULL, " +
                        "role VARCHAR(16) NOT NULL DEFAULT 'member', " +
                        "join_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "UNIQUE KEY uk_team_user (team_id, user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员'");
                log.info("[DbMigration] 已创建 t_team_member 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建团队表失败: {}", e.getMessage());
        }
    }

    /** 站内信与团队邀请表(幂等建表) */
    private void ensureNotificationTables() {
        try {
            Integer c1 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_notification'",
                    Integer.class);
            if (c1 == null || c1 == 0) {
                jdbcTemplate.execute("CREATE TABLE t_notification (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id BIGINT NOT NULL, " +
                        "type VARCHAR(32) NOT NULL DEFAULT 'system', " +
                        "title VARCHAR(128) DEFAULT NULL, " +
                        "content VARCHAR(500) DEFAULT NULL, " +
                        "data VARCHAR(500) DEFAULT NULL, " +
                        "is_read TINYINT(1) NOT NULL DEFAULT 0, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "KEY idx_notif_user (user_id, is_read)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信'");
                log.info("[DbMigration] 已创建 t_notification 表");
            }
            Integer c2 = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_team_invite'",
                    Integer.class);
            if (c2 == null || c2 == 0) {
                jdbcTemplate.execute("CREATE TABLE t_team_invite (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "team_id BIGINT NOT NULL, " +
                        "user_id BIGINT NOT NULL, " +
                        "status VARCHAR(16) NOT NULL DEFAULT 'PENDING', " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "handle_time DATETIME DEFAULT NULL, " +
                        "UNIQUE KEY uk_team_invite (team_id, user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队邀请'");
                log.info("[DbMigration] 已创建 t_team_invite 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建站内信/邀请表失败: {}", e.getMessage());
        }
    }

    /** 用户反馈表(幂等建表): 公开反馈墙 + 管理员回复 */
    private void ensureFeedbackTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_feedback'",
                    Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE TABLE t_feedback (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "user_id BIGINT NOT NULL, " +
                        "category VARCHAR(32) NOT NULL DEFAULT 'other', " +
                        "content TEXT NOT NULL, " +
                        "images LONGTEXT DEFAULT NULL, " +
                        "contact VARCHAR(128) DEFAULT NULL, " +
                        "status VARCHAR(16) NOT NULL DEFAULT 'PENDING', " +
                        "reply TEXT DEFAULT NULL, " +
                        "reply_user_id BIGINT DEFAULT NULL, " +
                        "reply_time DATETIME DEFAULT NULL, " +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                        "KEY idx_feedback_user (user_id), " +
                        "KEY idx_feedback_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈'");
                log.info("[DbMigration] 已创建 t_feedback 表");
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 创建 t_feedback 表失败: {}", e.getMessage());
        }
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

    /** 初始化管理后台菜单(按 id 幂等增量补齐, 已有库也会补上新菜单) */
    private void ensureMenus() {
        try {
            Map<String, Object[]> rows = seedMenus();
            int inserted = 0;
            LocalDateTime now = LocalDateTime.now();
            for (Map.Entry<String, Object[]> e : rows.entrySet()) {
                Long id = Long.valueOf(e.getKey());
                if (menuMapper.selectById(id) != null) {
                    continue;
                }
                Object[] v = e.getValue();
                Menu menu = new Menu();
                menu.setId(id);
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
                inserted++;
            }
            if (inserted > 0) {
                log.info("[DbMigration] 已补齐 {} 个后台菜单", inserted);
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 初始化菜单失败: {}", e.getMessage());
        }
    }

    private Map<String, Object[]> seedMenus() {
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

        // ---- 内容管理 ----
        rows.put("1021", new Object[]{0L, "内容管理", "M", "content", null, null, "content", 4, "内容运营"});
        rows.put("1022", new Object[]{1021L, "模板市场审核", "C", "market", "admin/TemplateMarketManage", "system:market:list",
                "market", 1, "模板市场上架/推荐"});
        rows.put("1023", new Object[]{1022L, "上架/推荐", "F", null, null, "system:market:edit", null, 1, "上架推荐"});

        // ---- 日志审计 ----
        rows.put("1024", new Object[]{1005L, "操作日志", "C", "oper-log", "admin/OperLogManage", "system:log:oper",
                "log", 4, "操作日志"});
        rows.put("1025", new Object[]{1005L, "登录日志", "C", "login-log", "admin/LoginLogManage", "system:log:login",
                "login", 5, "登录日志"});

        // ---- 字典/参数/公告 ----
        rows.put("1026", new Object[]{1005L, "字典管理", "C", "dict", "admin/DictManage", "system:dict:list",
                "dict", 6, "字典管理"});
        rows.put("1027", new Object[]{1026L, "字典新增", "F", null, null, "system:dict:add", null, 1, "新增字典"});
        rows.put("1028", new Object[]{1026L, "字典编辑", "F", null, null, "system:dict:edit", null, 2, "编辑字典"});
        rows.put("1029", new Object[]{1026L, "字典删除", "F", null, null, "system:dict:delete", null, 3, "删除字典"});
        rows.put("1030", new Object[]{1005L, "参数设置", "C", "config", "admin/ConfigManage", "system:config:list",
                "config", 7, "系统参数"});
        rows.put("1031", new Object[]{1030L, "参数新增", "F", null, null, "system:config:add", null, 1, "新增参数"});
        rows.put("1032", new Object[]{1030L, "参数编辑", "F", null, null, "system:config:edit", null, 2, "编辑参数"});
        rows.put("1033", new Object[]{1030L, "参数删除", "F", null, null, "system:config:delete", null, 3, "删除参数"});
        rows.put("1034", new Object[]{1005L, "公告管理", "C", "notice", "admin/NoticeManage", "system:notice:list",
                "notice", 8, "通知公告"});
        rows.put("1035", new Object[]{1034L, "公告新增", "F", null, null, "system:notice:add", null, 1, "新增公告"});
        rows.put("1036", new Object[]{1034L, "公告编辑", "F", null, null, "system:notice:edit", null, 2, "编辑公告"});
        rows.put("1037", new Object[]{1034L, "公告删除", "F", null, null, "system:notice:delete", null, 3, "删除公告"});

        // ---- 业务按钮 ----
        rows.put("1038", new Object[]{1006L, "用户导出", "F", null, null, "system:user:export", null, 6, "导出用户"});
        rows.put("1039", new Object[]{1006L, "重置密码", "F", null, null, "system:user:resetPwd", null, 7, "重置密码"});
        rows.put("1040", new Object[]{1006L, "用户封禁", "F", null, null, "system:user:status", null, 8, "封禁/启用"});
        rows.put("1041", new Object[]{1002L, "模板导出", "F", null, null, "system:template:export", null, 2, "导出模板"});
        rows.put("1042", new Object[]{1004L, "任务导出", "F", null, null, "system:task:export", null, 1, "导出任务"});
        rows.put("1043", new Object[]{1004L, "任务重试", "F", null, null, "system:task:rerun", null, 2, "任务重试"});
        rows.put("1044", new Object[]{1004L, "任务取消", "F", null, null, "system:task:cancel", null, 3, "任务取消"});

        // ---- 数据备份 ----
        rows.put("1045", new Object[]{1005L, "数据备份", "C", "backup", "admin/BackupManage", "system:backup:list",
                "backup", 9, "数据库备份与恢复"});
        rows.put("1046", new Object[]{1045L, "立即备份", "F", null, null, "system:backup:create", null, 1, "立即备份"});
        rows.put("1047", new Object[]{1045L, "删除备份", "F", null, null, "system:backup:delete", null, 2, "删除备份"});

        // ---- 用户反馈 ----
        rows.put("1048", new Object[]{1021L, "用户反馈管理", "C", "feedback", "admin/FeedbackManage", "system:feedback:list",
                "message", 2, "用户反馈管理"});
        rows.put("1049", new Object[]{1048L, "反馈回复", "F", null, null, "system:feedback:reply", null, 1, "反馈回复"});
        rows.put("1050", new Object[]{1048L, "反馈删除", "F", null, null, "system:feedback:delete", null, 2, "反馈删除"});

        // ---- 项目案例 ----
        rows.put("1051", new Object[]{1021L, "项目案例", "C", "case", "admin/CaseManage", "system:case:list",
                "case", 3, "项目案例管理"});
        rows.put("1052", new Object[]{1051L, "案例新增", "F", null, null, "system:case:add", null, 1, "新增案例"});
        rows.put("1053", new Object[]{1051L, "案例编辑", "F", null, null, "system:case:edit", null, 2, "编辑案例"});
        rows.put("1054", new Object[]{1051L, "案例删除", "F", null, null, "system:case:delete", null, 3, "删除案例"});
        return rows;
    }

    /** 给超管角色补齐全部菜单(幂等, 已有菜单时仅补缺失) */
    private void ensureAdminMenus() {
        try {
            Role adminRole = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleKey, Role.KEY_ADMIN).last("LIMIT 1"));
            if (adminRole == null) {
                return;
            }
            List<Menu> allMenus = menuMapper.selectList(null);
            List<Long> granted = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenu>()
                            .eq(RoleMenu::getRoleId, adminRole.getId()))
                    .stream().map(RoleMenu::getMenuId).collect(java.util.stream.Collectors.toSet())
                    .stream().collect(java.util.stream.Collectors.toList());
            int added = 0;
            for (Menu m : allMenus) {
                if (!granted.contains(m.getId())) {
                    roleMenuMapper.insert(new RoleMenu(adminRole.getId(), m.getId()));
                    added++;
                }
            }
            if (added > 0) {
                log.info("[DbMigration] 已为超管角色补齐 {} 个菜单", added);
            }
        } catch (Exception e) {
            log.warn("[DbMigration] 授予超管菜单失败: {}", e.getMessage());
        }
    }

    /** 种子字典与系统参数 */
    private void seedDictsAndConfigs() {
        try {
            insertDictTypeIfMissing("论文类型", "thesis_type", "论文/毕设类型");
            insertDictDataIfMissing("thesis_type", "毕业论文", "毕业论文", 1);
            insertDictDataIfMissing("thesis_type", "课程设计", "课程设计", 2);
            insertDictDataIfMissing("thesis_type", "开题报告", "开题报告", 3);
            insertDictTypeIfMissing("模板类型", "template_type", "模板市场分类");
            insertDictDataIfMissing("template_type", "本科", "本科", 1);
            insertDictDataIfMissing("template_type", "硕士", "硕士", 2);
            insertDictDataIfMissing("template_type", "博士", "博士", 3);
            insertDictDataIfMissing("template_type", "期刊", "期刊", 4);

            insertConfigIfMissing("上传文件大小上限(MB)", "upload.max.size", "50", 1, "论文上传大小限制");
            insertConfigIfMissing("任务并发数", "task.max.concurrent", "4", 1, "排版任务最大并发");
            insertConfigIfMissing("文件保留天数", "storage.keep.days", "30", 1, "超过该天数的旧文件可被清理");
                    } catch (Exception e) {
            log.warn("[DbMigration] 初始化字典/参数失败: {}", e.getMessage());
        }
    }

    private void insertDictTypeIfMissing(String name, String type, String remark) {
        Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_dict_type WHERE dict_type = ?", Long.class, type);
        if (cnt == null || cnt == 0) {
            jdbcTemplate.update("INSERT INTO t_dict_type (dict_name, dict_type, status, remark, create_time, update_time) VALUES (?,?,1,?,NOW(),NOW())",
                    name, type, remark);
        }
    }

    private void insertDictDataIfMissing(String type, String label, String value, int sort) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_dict_data WHERE dict_type = ? AND dict_value = ?", Long.class, type, value);
        if (cnt == null || cnt == 0) {
            jdbcTemplate.update("INSERT INTO t_dict_data (dict_type, dict_label, dict_value, dict_sort, status, remark, create_time, update_time) VALUES (?,?,?,?,1,NULL,NOW(),NOW())",
                    type, label, value, sort);
        }
    }

    private void insertConfigIfMissing(String name, String key, String value, int type, String remark) {
        Long cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_config WHERE config_key = ?", Long.class, key);
        if (cnt == null || cnt == 0) {
            jdbcTemplate.update("INSERT INTO t_config (config_name, config_key, config_value, config_type, remark, create_time, update_time) VALUES (?,?,?,?,?,NOW(),NOW())",
                    name, key, value, type, remark);
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
