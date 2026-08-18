-- ============================================================
-- 论文格式助手 数据库初始化脚本(安全幂等版)
-- CREATE TABLE IF NOT EXISTS: 不删除已有表, 重启不会清空数据
-- 结构对齐生产库(2026-08-17 导出): 含团队/站内信/会话/评分等扩展表
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
    status            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
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
    is_public         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否上架模板市场',
    recommended       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否推荐',
    public_time       DATETIME     DEFAULT NULL COMMENT '上架时间',
    category          VARCHAR(50)  DEFAULT NULL COMMENT '市场分类',
    download_count    INT          NOT NULL DEFAULT 0 COMMENT '市场下载量',
    rating_avg        DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '平均评分',
    rating_count      INT          NOT NULL DEFAULT 0 COMMENT '评分人数',
    team_id           BIGINT       DEFAULT NULL COMMENT '所属团队(空=个人)',
    create_time       DATETIME     DEFAULT NULL,
    update_time       DATETIME     DEFAULT NULL,
    KEY idx_user (user_id),
    KEY idx_public (is_public)
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
    team_id       BIGINT      DEFAULT NULL COMMENT '所属团队(空=个人)',
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
    retry_count INT          NOT NULL DEFAULT 0 COMMENT '失败自动重试次数',
    summary     VARCHAR(500) DEFAULT NULL COMMENT '排版校验摘要',
    team_id     BIGINT       DEFAULT NULL COMMENT '所属团队(空=个人)',
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

-- ============================================================
-- 运维扩展: 操作日志 / 登录日志 / 字典 / 系统参数 / 公告
-- ============================================================

CREATE TABLE IF NOT EXISTS t_oper_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       DEFAULT NULL,
    username    VARCHAR(64)  DEFAULT NULL,
    module      VARCHAR(64)  DEFAULT NULL,
    action      VARCHAR(128) DEFAULT NULL,
    method      VARCHAR(255) DEFAULT NULL,
    params      TEXT         DEFAULT NULL,
    ip          VARCHAR(64)  DEFAULT NULL,
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    error_msg   VARCHAR(1000) DEFAULT NULL,
    cost_ms     BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    KEY idx_user (user_id),
    KEY idx_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

CREATE TABLE IF NOT EXISTS t_login_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       DEFAULT NULL,
    username    VARCHAR(64)  DEFAULT NULL,
    ip          VARCHAR(64)  DEFAULT NULL,
    user_agent  VARCHAR(255) DEFAULT NULL,
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    message     VARCHAR(255) DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    KEY idx_user (user_id),
    KEY idx_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

CREATE TABLE IF NOT EXISTS t_dict_type (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_name   VARCHAR(100) NOT NULL,
    dict_type   VARCHAR(100) NOT NULL,
    status      TINYINT(1)   NOT NULL DEFAULT 1,
    remark      VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典类型表';

CREATE TABLE IF NOT EXISTS t_dict_data (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(100) NOT NULL,
    dict_label  VARCHAR(100) NOT NULL,
    dict_value  VARCHAR(100) NOT NULL,
    dict_sort   INT          NOT NULL DEFAULT 0,
    status      TINYINT(1)   NOT NULL DEFAULT 1,
    remark      VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL,
    KEY idx_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='字典数据表';

CREATE TABLE IF NOT EXISTS t_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_name  VARCHAR(100) NOT NULL,
    config_key   VARCHAR(100) NOT NULL,
    config_value VARCHAR(500) DEFAULT NULL,
    config_type  TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1内置 0自定义',
    remark       VARCHAR(500) DEFAULT NULL,
    create_time  DATETIME     DEFAULT NULL,
    update_time  DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统参数表';

CREATE TABLE IF NOT EXISTS t_notice (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         DEFAULT NULL,
    notice_type CHAR(1)      NOT NULL DEFAULT '1' COMMENT '1通知 2公告',
    status      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1正常 0停用',
    create_user BIGINT       DEFAULT NULL,
    create_time DATETIME     DEFAULT NULL,
    update_time DATETIME     DEFAULT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='通知公告表';

-- ============================================================
-- 扩展功能: 登录会话 / 模板市场评分与收藏 / 团队协作 / 站内信
-- ============================================================

-- 登录会话表: 记录活跃登录, 支持后台查看在线用户与强制下线
CREATE TABLE IF NOT EXISTS t_login_session (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    username    VARCHAR(64)  DEFAULT NULL,
    token       VARCHAR(600) NOT NULL,
    ip          VARCHAR(64)  DEFAULT NULL,
    login_time  DATETIME     DEFAULT NULL,
    expire_time BIGINT       NOT NULL,
    KEY idx_login_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='登录会话';

-- 模板市场评分表: 每个用户对每个市场模板最多一条评分
CREATE TABLE IF NOT EXISTS t_market_rating (
    user_id     BIGINT   NOT NULL,
    template_id BIGINT   NOT NULL,
    score       TINYINT  NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='模板市场评分';

-- 模板收藏表: 用户对市场模板的收藏
CREATE TABLE IF NOT EXISTS t_template_favorite (
    user_id     BIGINT   NOT NULL,
    template_id BIGINT   NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='模板收藏';

-- 团队表: 多人协作, 团队内共享模板/论文/任务
CREATE TABLE IF NOT EXISTS t_team (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    owner_id    BIGINT       NOT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队';

-- 团队成员表
CREATE TABLE IF NOT EXISTS t_team_member (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id   BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    role      VARCHAR(16) NOT NULL DEFAULT 'member',
    join_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_user (team_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队成员';

-- 团队邀请表
CREATE TABLE IF NOT EXISTS t_team_invite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id     BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    handle_time DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_team_invite (team_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团队邀请';

-- 站内信表
CREATE TABLE IF NOT EXISTS t_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(32)  NOT NULL DEFAULT 'system',
    title       VARCHAR(128) DEFAULT NULL,
    content     VARCHAR(500) DEFAULT NULL,
    data        VARCHAR(500) DEFAULT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notif_user (user_id, is_read)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='站内信';