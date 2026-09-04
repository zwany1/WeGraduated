-- 个人资料扩展: t_user 加 bio/gender/school/major/city/phone 6 个可选列
-- 幂等: 重复执行不报错; 新部署数据库若已是最新,直接跳过。
-- 与 Profile.vue / UserProfileDTO / User 实体字段对齐。

SET @col_bio := (SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME   = 't_user'
                   AND COLUMN_NAME  = 'bio');
SET @sql := IF(@col_bio = 0,
  'ALTER TABLE t_user ADD COLUMN bio VARCHAR(200) DEFAULT NULL COMMENT ''个人简介''',
  'SELECT ''bio exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_gender := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME   = 't_user'
                      AND COLUMN_NAME  = 'gender');
SET @sql := IF(@col_gender = 0,
  'ALTER TABLE t_user ADD COLUMN gender TINYINT DEFAULT NULL COMMENT ''性别 0不填 1男 2女''',
  'SELECT ''gender exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_school := (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME   = 't_user'
                       AND COLUMN_NAME  = 'school');
SET @sql := IF(@col_school = 0,
  'ALTER TABLE t_user ADD COLUMN school VARCHAR(80) DEFAULT NULL COMMENT ''学校/单位''',
  'SELECT ''school exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_major := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME   = 't_user'
                      AND COLUMN_NAME  = 'major');
SET @sql := IF(@col_major = 0,
  'ALTER TABLE t_user ADD COLUMN major VARCHAR(80) DEFAULT NULL COMMENT ''学院/专业''',
  'SELECT ''major exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_city := (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME   = 't_user'
                     AND COLUMN_NAME  = 'city');
SET @sql := IF(@col_city = 0,
  'ALTER TABLE t_user ADD COLUMN city VARCHAR(40) DEFAULT NULL COMMENT ''常驻城市''',
  'SELECT ''city exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_phone := (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME   = 't_user'
                      AND COLUMN_NAME  = 'phone');
SET @sql := IF(@col_phone = 0,
  'ALTER TABLE t_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT ''联系电话''',
  'SELECT ''phone exists, skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;