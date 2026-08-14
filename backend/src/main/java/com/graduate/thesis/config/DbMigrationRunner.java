package com.graduate.thesis.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduate.thesis.entity.User;
import com.graduate.thesis.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动迁移: 幂等补齐 t_user.role 列, 并确保初始管理员账号存在。
 * 兼容已有数据库(CREATE TABLE IF NOT EXISTS 不会给老表补列)。
 */
@Slf4j
@Component
public class DbMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final String adminUsername;
    private final String adminPassword;

    public DbMigrationRunner(JdbcTemplate jdbcTemplate,
                             UserMapper userMapper,
                             @Value("${thesis.admin.username:admin}") String adminUsername,
                             @Value("${thesis.admin.password:admin123}") String adminPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRoleColumn();
        ensureAdminAccount();
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
}
