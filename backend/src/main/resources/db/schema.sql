-- ============================================================
-- 论文格式助手 数据库初始化脚本(安全幂等版)
-- CREATE TABLE IF NOT EXISTS: 不删除已有表, 重启不会清空数据
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    username          VARCHAR(32)  NOT NULL,
    password          VARCHAR(64)  NOT NULL,
    email             VARCHAR(128) DEFAULT NULL,
    nickname          VARCHAR(64)  DEFAULT NULL,
    avatar            LONGTEXT     DEFAULT NULL,
    security_question VARCHAR(128) DEFAULT NULL,
    security_answer   VARCHAR(128) DEFAULT NULL,
    role              VARCHAR(16)  NOT NULL DEFAULT 'USER',
    create_time       DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS t_format_template (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    name              VARCHAR(64)  NOT NULL,
    page_config       TEXT         DEFAULT NULL,
    heading_patterns  TEXT         DEFAULT NULL,
    cover_config      TEXT         DEFAULT NULL,
    generate_toc      TINYINT(1)   NOT NULL DEFAULT 0,
    reference_config  TEXT         DEFAULT NULL,
    create_time       DATETIME     DEFAULT NULL,
    update_time       DATETIME     DEFAULT NULL,
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='格式模板表';

CREATE TABLE IF NOT EXISTS t_format_rule (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id        BIGINT       NOT NULL,
    rule_type          VARCHAR(32)  NOT NULL,
    font               VARCHAR(64)  DEFAULT NULL,
    font_latin         VARCHAR(64)  DEFAULT NULL,
    font_size          INT          DEFAULT NULL,
    bold               TINYINT(1)   DEFAULT NULL,
    align              VARCHAR(16)  DEFAULT NULL,
    line_spacing       FLOAT        DEFAULT NULL,
    line_spacing_type  VARCHAR(16)  DEFAULT 'multiple',
    line_spacing_exact INT          DEFAULT NULL,
    first_line_indent  INT          DEFAULT NULL,
    space_before       INT          DEFAULT NULL,
    space_after        INT          DEFAULT NULL,
    caption_position   VARCHAR(16)  DEFAULT NULL,
    caption_enabled    TINYINT(1)   DEFAULT NULL,
    numbering_pattern  VARCHAR(64)  DEFAULT NULL,
    create_time        DATETIME     DEFAULT NULL,
    update_time        DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_template_rule (template_id, rule_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='格式规则表';

CREATE TABLE IF NOT EXISTS t_paper_file (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_path   VARCHAR(255) NOT NULL,
    file_size     BIGINT      DEFAULT NULL,
    create_time   DATETIME    DEFAULT NULL,
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='论文文件表';

CREATE TABLE IF NOT EXISTS t_format_task (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    file_id     BIGINT      NOT NULL,
    template_id BIGINT      NOT NULL,
    status      VARCHAR(16) NOT NULL,
    progress    INT         DEFAULT 0,
    result_path VARCHAR(255) DEFAULT NULL,
    pdf_path    VARCHAR(255) DEFAULT NULL,
    error_msg   TEXT         DEFAULT NULL,
    create_time DATETIME    DEFAULT NULL,
    finish_time DATETIME    DEFAULT NULL,
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='排版任务表';

-- ============================================================
-- RBAC 权限体系: 菜单 / 角色 / 用户角色 / 角色菜单
-- ============================================================

-- 菜单表(M目录 / C菜单 / F按钮)
CREATE TABLE IF NOT EXISTS t_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    menu_name   VARCHAR(64)  NOT NULL,
    menu_type   CHAR(1)      NOT NULL DEFAULT 'M' COMMENT 'M目录 C菜单 F按钮',
    path        VARCHAR(200) DEFAULT NULL,
    component   VARCHAR(255) DEFAULT NULL,
    perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识, 如 system:user:add',
    icon        VARCHAR(100) DEFAULT NULL,
    order_num   INT          NOT NULL DEFAULT 0,
    visible     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '显示状态 1显示 0隐藏',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    KEY idx_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜单权限表';

-- 角色表
CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(32)  NOT NULL,
    role_key    VARCHAR(32)  NOT NULL COMMENT '角色权限字符串',
    remark      VARCHAR(255) DEFAULT NULL,
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS t_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS t_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_menu (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色菜单关联表';
