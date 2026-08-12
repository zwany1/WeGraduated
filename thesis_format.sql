/*
 Navicat Premium Dump SQL

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : thesis_format

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 07/08/2026 16:36:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_format_rule
-- ----------------------------
DROP TABLE IF EXISTS `t_format_rule`;
CREATE TABLE `t_format_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `rule_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `font` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `font_size` int NULL DEFAULT NULL,
  `bold` tinyint(1) NULL DEFAULT NULL,
  `align` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `line_spacing` float NULL DEFAULT NULL,
  `first_line_indent` int NULL DEFAULT NULL,
  `space_before` int NULL DEFAULT NULL,
  `space_after` int NULL DEFAULT NULL,
  `caption_position` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `numbering_pattern` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `font_latin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `line_spacing_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'multiple',
  `line_spacing_exact` int NULL DEFAULT NULL,
  `caption_enabled` tinyint(1) NULL DEFAULT 1 COMMENT '图表题注编号开关',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_template_rule`(`template_id` ASC, `rule_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 245 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏍煎紡瑙勫垯琛�' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_format_rule
-- ----------------------------
INSERT INTO `t_format_rule` VALUES (183, 42, 'heading1', '黑体', 14, 1, 'left', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 15:30:41', '2026-08-05 15:50:14', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (184, 42, 'heading2', '黑体', 12, 0, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 15:30:41', '2026-08-05 15:50:14', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (185, 42, 'heading3', '楷体', 12, 0, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 15:30:41', '2026-08-05 15:50:14', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (186, 42, 'body', '宋体', 12, NULL, 'justify', NULL, 2, NULL, 0, NULL, NULL, '2026-08-05 15:30:41', '2026-08-05 15:50:14', 'Times New Roman', 'exact', 20, 1);
INSERT INTO `t_format_rule` VALUES (187, 42, 'figure', '宋体', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', '图{chapter}-{no}', '2026-08-05 15:30:41', '2026-08-05 15:50:15', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (188, 42, 'table', '宋体', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', '表{chapter}-{no}', '2026-08-05 15:30:41', '2026-08-05 15:50:15', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (203, 47, 'heading1', 'Hei', 16, 1, 'center', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (204, 47, 'heading2', 'Hei', 14, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (205, 47, 'heading3', 'Hei', 12, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (206, 47, 'body', 'Song', 12, NULL, 'justify', 1.5, 2, NULL, NULL, NULL, NULL, '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (207, 47, 'figure', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', 'fig', '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (208, 47, 'table', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', 'tab', '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (209, 48, 'heading1', 'Hei', 16, 1, 'center', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (210, 48, 'heading2', 'Hei', 14, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (211, 48, 'heading3', 'Hei', 12, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (212, 48, 'body', 'Song', 12, NULL, 'justify', 1.5, 2, NULL, NULL, NULL, NULL, '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (213, 48, 'figure', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', 'fig', '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (214, 48, 'table', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', 'tab', '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (215, 49, 'heading1', 'Hei', 16, 1, 'center', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (216, 49, 'heading2', 'Hei', 14, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (217, 49, 'heading3', 'Hei', 12, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (218, 49, 'body', 'Song', 12, NULL, 'justify', 1.5, 2, NULL, NULL, NULL, NULL, '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (219, 49, 'figure', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', 'fig', '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (220, 49, 'table', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', 'tab', '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (221, 50, 'heading1', 'Hei', 16, 1, 'center', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (222, 50, 'heading2', 'Hei', 14, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (223, 50, 'heading3', 'Hei', 12, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (224, 50, 'body', 'Song', 12, NULL, 'justify', 1.5, 2, NULL, NULL, NULL, NULL, '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (225, 50, 'figure', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', '图{chapter}-{no}', '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (226, 50, 'table', 'Song', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', '表{chapter}-{no}', '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (227, 53, 'heading1', 'HeiTi', 16, 1, 'center', NULL, NULL, 12, 12, NULL, NULL, '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (228, 53, 'heading2', 'HeiTi', 14, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (229, 53, 'heading3', 'KaiTi', 12, NULL, 'left', NULL, NULL, NULL, NULL, NULL, NULL, '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (230, 53, 'body', 'SongTi', 12, NULL, 'justify', 1.5, 2, NULL, NULL, NULL, NULL, '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 1);
INSERT INTO `t_format_rule` VALUES (231, 53, 'figure', 'SongTi', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'below', 'figure-{chapter}-{no}', '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 0);
INSERT INTO `t_format_rule` VALUES (232, 53, 'table', 'SongTi', 10, NULL, NULL, NULL, NULL, NULL, NULL, 'above', 'table-{chapter}-{no}', '2026-08-05 16:53:10', '2026-08-05 16:53:10', 'Times New Roman', 'multiple', NULL, 0);

-- ----------------------------
-- Table structure for t_format_task
-- ----------------------------
DROP TABLE IF EXISTS `t_format_task`;
CREATE TABLE `t_format_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `file_id` bigint NOT NULL,
  `template_id` bigint NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `progress` int NULL DEFAULT 0,
  `result_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `finish_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎺掔増浠诲姟琛�' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_format_task
-- ----------------------------
INSERT INTO `t_format_task` VALUES (1, 1, 1, 1, 'FAILED', 65, NULL, '旧版本任务已失效', '2026-08-05 10:35:54', NULL);
INSERT INTO `t_format_task` VALUES (2, 1, 2, 2, 'FAILED', 65, NULL, '文档处理失败: Document nodes may not be imported', '2026-08-05 10:42:13', '2026-08-05 10:42:14');
INSERT INTO `t_format_task` VALUES (3, 1, 3, 3, 'SUCCESS', 100, 'result/20260805/result_1_7fecc1cea87f.docx', NULL, '2026-08-05 10:45:22', '2026-08-05 10:45:23');
INSERT INTO `t_format_task` VALUES (4, 1, 4, 3, 'SUCCESS', 100, 'result/20260805/result_1_ed36f6700723.docx', NULL, '2026-08-05 10:57:58', '2026-08-05 10:57:59');
INSERT INTO `t_format_task` VALUES (5, 1, 5, 4, 'SUCCESS', 100, 'result/20260805/result_1_f03ca7c81ddc.docx', NULL, '2026-08-05 11:08:51', '2026-08-05 11:08:52');
INSERT INTO `t_format_task` VALUES (6, 1, 6, 5, 'SUCCESS', 100, 'result/20260805/result_1_ee3560e6e559.docx', NULL, '2026-08-05 11:10:04', '2026-08-05 11:10:04');
INSERT INTO `t_format_task` VALUES (7, 1, 7, 6, 'SUCCESS', 100, 'result/20260805/result_1_305adb83bd3e.docx', NULL, '2026-08-05 11:10:05', '2026-08-05 11:10:05');
INSERT INTO `t_format_task` VALUES (8, 1, 8, 7, 'SUCCESS', 100, 'result/20260805/result_1_6fc35d34fb7d.docx', NULL, '2026-08-05 11:14:14', '2026-08-05 11:14:15');
INSERT INTO `t_format_task` VALUES (9, 1, 9, 8, 'SUCCESS', 100, 'result/20260805/result_1_f6009dce04a2.docx', NULL, '2026-08-05 11:14:16', '2026-08-05 11:14:16');
INSERT INTO `t_format_task` VALUES (10, 1, 10, 9, 'SUCCESS', 100, 'result/20260805/result_1_11ef12e43953.docx', NULL, '2026-08-05 11:14:59', '2026-08-05 11:14:59');
INSERT INTO `t_format_task` VALUES (11, 1, 11, 10, 'SUCCESS', 100, 'result/20260805/result_1_31e4385bcf26.docx', NULL, '2026-08-05 11:15:00', '2026-08-05 11:15:00');
INSERT INTO `t_format_task` VALUES (12, 1, 12, 11, 'SUCCESS', 100, 'result/20260805/result_1_af057bda9ca5.docx', NULL, '2026-08-05 11:15:08', '2026-08-05 11:15:08');
INSERT INTO `t_format_task` VALUES (13, 1, 13, 12, 'SUCCESS', 100, 'result/20260805/result_1_c2b76e7d84ec.docx', NULL, '2026-08-05 11:39:11', '2026-08-05 11:39:13');
INSERT INTO `t_format_task` VALUES (14, 1, 14, 13, 'SUCCESS', 100, 'result/20260805/result_1_d3acf9f8178c.docx', NULL, '2026-08-05 11:41:26', '2026-08-05 11:41:28');
INSERT INTO `t_format_task` VALUES (15, 1, 15, 14, 'SUCCESS', 100, 'result/20260805/result_1_0c751aea1aeb.docx', NULL, '2026-08-05 11:49:01', '2026-08-05 11:49:02');
INSERT INTO `t_format_task` VALUES (16, 1, 16, 15, 'SUCCESS', 100, 'result/20260805/result_1_42d38745cf93.docx', NULL, '2026-08-05 11:55:18', '2026-08-05 11:55:19');
INSERT INTO `t_format_task` VALUES (17, 1, 17, 16, 'SUCCESS', 100, 'result/20260805/result_1_159955f5cbe8.docx', NULL, '2026-08-05 11:55:24', '2026-08-05 11:55:24');
INSERT INTO `t_format_task` VALUES (18, 1, 18, 17, 'SUCCESS', 100, 'result/20260805/result_1_8badbf7bc340.docx', NULL, '2026-08-05 12:01:27', '2026-08-05 12:01:28');
INSERT INTO `t_format_task` VALUES (19, 1, 19, 18, 'SUCCESS', 100, 'result/20260805/result_1_66500417f9a3.docx', NULL, '2026-08-05 12:04:13', '2026-08-05 12:04:14');
INSERT INTO `t_format_task` VALUES (20, 1, 20, 19, 'FAILED', 65, NULL, '文档处理失败: A part with the name \'/word/styles.xml\' already exists : Packages shall not contain equivalent part names and package implementers shall neither create nor recognize packages with equivalent part names. [M1.12]', '2026-08-05 12:04:19', '2026-08-05 12:04:19');
INSERT INTO `t_format_task` VALUES (21, 1, 21, 20, 'SUCCESS', 100, 'result/20260805/result_1_b5e099777d49.docx', NULL, '2026-08-05 12:06:41', '2026-08-05 12:06:42');
INSERT INTO `t_format_task` VALUES (22, 1, 22, 21, 'SUCCESS', 100, 'result/20260805/result_1_1cb3e467263c.docx', NULL, '2026-08-05 12:06:46', '2026-08-05 12:06:46');
INSERT INTO `t_format_task` VALUES (23, 1, 23, 21, 'SUCCESS', 100, 'result/20260805/result_1_403689b08f0a.docx', NULL, '2026-08-05 14:26:46', '2026-08-05 14:26:46');
INSERT INTO `t_format_task` VALUES (24, 1, 20, 19, 'SUCCESS', 100, 'result/20260805/result_1_a5b503c92e09.docx', NULL, '2026-08-05 14:28:49', '2026-08-05 14:28:49');
INSERT INTO `t_format_task` VALUES (25, 1, 20, 19, 'SUCCESS', 100, 'result/20260805/result_1_6c9173a055f3.docx', NULL, '2026-08-05 14:28:56', '2026-08-05 14:28:56');
INSERT INTO `t_format_task` VALUES (26, 1, 24, 22, 'SUCCESS', 100, 'result/20260805/result_1_11369ecbb5b5.docx', NULL, '2026-08-05 14:35:29', '2026-08-05 14:35:29');
INSERT INTO `t_format_task` VALUES (27, 1, 25, 23, 'SUCCESS', 100, 'result/20260805/result_1_06cd9f6295a5.docx', NULL, '2026-08-05 14:35:41', '2026-08-05 14:35:41');
INSERT INTO `t_format_task` VALUES (28, 1, 26, 23, 'SUCCESS', 100, 'result/20260805/result_1_b0571d07e939.docx', NULL, '2026-08-05 14:35:42', '2026-08-05 14:35:42');
INSERT INTO `t_format_task` VALUES (29, 1, 27, 25, 'SUCCESS', 100, 'result/20260805/result_1_7a8d908d3f6e.docx', NULL, '2026-08-05 14:38:51', '2026-08-05 14:38:52');
INSERT INTO `t_format_task` VALUES (30, 1, 28, 25, 'SUCCESS', 100, 'result/20260805/result_1_36c724c6ee82.docx', NULL, '2026-08-05 14:38:53', '2026-08-05 14:38:53');
INSERT INTO `t_format_task` VALUES (31, 1, 29, 26, 'SUCCESS', 100, 'result/20260805/result_1_7cfa3820f74a.docx', NULL, '2026-08-05 14:39:11', '2026-08-05 14:39:11');
INSERT INTO `t_format_task` VALUES (32, 1, 30, 26, 'SUCCESS', 100, 'result/20260805/result_1_7e249f1c8713.docx', NULL, '2026-08-05 14:39:12', '2026-08-05 14:39:12');
INSERT INTO `t_format_task` VALUES (33, 1, 31, 27, 'SUCCESS', 100, 'result/20260805/result_1_3aaa84c69ff2.docx', NULL, '2026-08-05 14:39:19', '2026-08-05 14:39:20');
INSERT INTO `t_format_task` VALUES (34, 1, 32, 28, 'SUCCESS', 100, 'result/20260805/result_1_308db7ad72a3.docx', NULL, '2026-08-05 14:39:21', '2026-08-05 14:39:21');
INSERT INTO `t_format_task` VALUES (35, 1, 33, 29, 'SUCCESS', 100, 'result/20260805/result_1_610aa287093c.docx', NULL, '2026-08-05 14:48:37', '2026-08-05 14:48:38');
INSERT INTO `t_format_task` VALUES (36, 1, 34, 29, 'SUCCESS', 100, 'result/20260805/result_1_bd2cc9ff357b.docx', NULL, '2026-08-05 14:48:39', '2026-08-05 14:48:39');
INSERT INTO `t_format_task` VALUES (37, 1, 35, 30, 'SUCCESS', 100, 'result/20260805/result_1_d1da3125b5a4.docx', NULL, '2026-08-05 14:49:11', '2026-08-05 14:49:11');
INSERT INTO `t_format_task` VALUES (38, 1, 36, 31, 'SUCCESS', 100, 'result/20260805/result_1_878e8658829b.docx', NULL, '2026-08-05 14:49:13', '2026-08-05 14:49:13');
INSERT INTO `t_format_task` VALUES (39, 1, 37, 31, 'SUCCESS', 100, 'result/20260805/result_1_517362fa9ea3.docx', NULL, '2026-08-05 14:49:14', '2026-08-05 14:49:15');
INSERT INTO `t_format_task` VALUES (40, 1, 38, 32, 'SUCCESS', 100, 'result/20260805/result_1_d4e8d9af60f6.docx', NULL, '2026-08-05 14:49:25', '2026-08-05 14:49:25');
INSERT INTO `t_format_task` VALUES (41, 1, 39, 32, 'SUCCESS', 100, 'result/20260805/result_1_39d9db231aa3.docx', NULL, '2026-08-05 14:56:02', '2026-08-05 14:56:04');
INSERT INTO `t_format_task` VALUES (42, 1, 40, 32, 'SUCCESS', 100, 'result/20260805/result_1_9f95ba0fd320.docx', NULL, '2026-08-05 14:59:55', '2026-08-05 14:59:56');
INSERT INTO `t_format_task` VALUES (43, 1, 41, 32, 'SUCCESS', 100, 'result/20260805/result_1_e66b6544976c.docx', NULL, '2026-08-05 15:06:03', '2026-08-05 15:06:06');
INSERT INTO `t_format_task` VALUES (44, 1, 42, 33, 'SUCCESS', 100, 'result/20260805/result_1_3d34d1f65783.docx', NULL, '2026-08-05 15:06:12', '2026-08-05 15:06:12');
INSERT INTO `t_format_task` VALUES (45, 1, 43, 34, 'SUCCESS', 100, 'result/20260805/result_1_be1e51c9dc78.docx', NULL, '2026-08-05 15:06:38', '2026-08-05 15:06:38');
INSERT INTO `t_format_task` VALUES (46, 1, 44, 35, 'SUCCESS', 100, 'result/20260805/result_1_b5f2b39d089b.docx', NULL, '2026-08-05 15:18:28', '2026-08-05 15:18:30');
INSERT INTO `t_format_task` VALUES (47, 1, 45, 36, 'SUCCESS', 100, 'result/20260805/result_1_b1f19e6b1461.docx', NULL, '2026-08-05 15:21:33', '2026-08-05 15:21:34');
INSERT INTO `t_format_task` VALUES (48, 1, 46, 37, 'SUCCESS', 100, 'result/20260805/result_1_283fdf463a20.docx', NULL, '2026-08-05 15:22:25', '2026-08-05 15:22:25');
INSERT INTO `t_format_task` VALUES (49, 1, 47, 38, 'SUCCESS', 100, 'result/20260805/result_1_ce1cbca332db.docx', NULL, '2026-08-05 15:22:33', '2026-08-05 15:22:34');
INSERT INTO `t_format_task` VALUES (50, 1, 48, 39, 'SUCCESS', 100, 'result/20260805/result_1_4f0b8f4d2f48.docx', NULL, '2026-08-05 15:22:35', '2026-08-05 15:22:35');
INSERT INTO `t_format_task` VALUES (51, 1, 49, 40, 'SUCCESS', 100, 'result/20260805/result_1_fb2bd76e6e5d.docx', NULL, '2026-08-05 15:30:26', '2026-08-05 15:30:27');
INSERT INTO `t_format_task` VALUES (52, 1, 50, 41, 'SUCCESS', 100, 'result/20260805/result_1_a78002dbe6d8.docx', NULL, '2026-08-05 15:30:39', '2026-08-05 15:30:40');
INSERT INTO `t_format_task` VALUES (53, 1, 51, 42, 'SUCCESS', 100, 'result/20260805/result_1_b14adc3acaee.docx', NULL, '2026-08-05 15:30:41', '2026-08-05 15:30:41');
INSERT INTO `t_format_task` VALUES (54, 1, 52, 42, 'SUCCESS', 100, 'result/20260805/result_1_d47ad5b38452.docx', NULL, '2026-08-05 15:31:24', '2026-08-05 15:31:25');
INSERT INTO `t_format_task` VALUES (55, 1, 53, 42, 'SUCCESS', 100, 'result/20260805/result_1_a7e9a9357466.docx', NULL, '2026-08-05 15:32:56', '2026-08-05 15:32:57');
INSERT INTO `t_format_task` VALUES (56, 1, 54, 43, 'SUCCESS', 100, 'result/20260805/result_1_55ed842d7744.docx', NULL, '2026-08-05 15:39:36', '2026-08-05 15:39:37');
INSERT INTO `t_format_task` VALUES (57, 1, 55, 42, 'SUCCESS', 100, 'result/20260805/result_1_0a2a94110b99.docx', NULL, '2026-08-05 15:42:56', '2026-08-05 15:42:58');
INSERT INTO `t_format_task` VALUES (58, 1, 56, 42, 'SUCCESS', 100, 'result/20260805/result_1_e334a378d378.docx', NULL, '2026-08-05 15:50:23', '2026-08-05 15:50:25');
INSERT INTO `t_format_task` VALUES (59, 1, 57, 44, 'SUCCESS', 100, 'result/20260805/result_1_64ea92574647.docx', NULL, '2026-08-05 16:00:15', '2026-08-05 16:00:15');
INSERT INTO `t_format_task` VALUES (60, 1, 58, 45, 'SUCCESS', 100, 'result/20260805/result_1_d1a41fa25a40.docx', NULL, '2026-08-05 16:00:37', '2026-08-05 16:00:37');
INSERT INTO `t_format_task` VALUES (61, 1, 59, 42, 'SUCCESS', 100, 'result/20260805/result_1_6eafd084dfdf.docx', NULL, '2026-08-05 16:03:27', '2026-08-05 16:03:28');
INSERT INTO `t_format_task` VALUES (62, 1, 60, 46, 'SUCCESS', 100, 'result/20260805/result_1_bc28cbc67f2c.docx', NULL, '2026-08-05 16:24:04', '2026-08-05 16:24:05');
INSERT INTO `t_format_task` VALUES (63, 1, 59, 42, 'SUCCESS', 100, 'result/20260805/result_1_54148f160a90.docx', NULL, '2026-08-05 16:24:17', '2026-08-05 16:24:19');
INSERT INTO `t_format_task` VALUES (64, 1, 61, 42, 'SUCCESS', 100, 'result/20260805/result_1_08dfdf96c0d5.docx', NULL, '2026-08-05 16:24:47', '2026-08-05 16:24:48');
INSERT INTO `t_format_task` VALUES (65, 1, 62, 42, 'SUCCESS', 100, 'result/20260805/result_1_599a200bfe0a.docx', NULL, '2026-08-05 16:25:13', '2026-08-05 16:25:13');
INSERT INTO `t_format_task` VALUES (66, 2, 63, 47, 'FAILED', 5, NULL, '文档处理失败: org.apache.xmlbeans.XmlException: error: The document is not a document@http://schemas.openxmlformats.org/wordprocessingml/2006/main: document element local name mismatch expected document got body', '2026-08-05 16:37:13', '2026-08-05 16:37:14');
INSERT INTO `t_format_task` VALUES (67, 2, 64, 48, 'SUCCESS', 100, 'result/20260805/result_2_eeda680e8a32.docx', NULL, '2026-08-05 16:37:49', '2026-08-05 16:37:50');
INSERT INTO `t_format_task` VALUES (68, 2, 65, 49, 'SUCCESS', 100, 'result/20260805/result_2_f1b9bc735e38.docx', NULL, '2026-08-05 16:40:15', '2026-08-05 16:40:15');
INSERT INTO `t_format_task` VALUES (69, 2, 66, 50, 'SUCCESS', 100, 'result/20260805/result_2_99673d2b07b6.docx', NULL, '2026-08-05 16:41:51', '2026-08-05 16:41:51');
INSERT INTO `t_format_task` VALUES (70, 1, 67, 54, 'SUCCESS', 100, 'result/20260805/result_1_410519bbdc74.docx', NULL, '2026-08-05 16:53:33', '2026-08-05 16:53:34');
INSERT INTO `t_format_task` VALUES (71, 1, 68, 55, 'SUCCESS', 100, 'result/20260805/result_1_c11677aa51d2.docx', NULL, '2026-08-05 16:54:33', '2026-08-05 16:54:33');

-- ----------------------------
-- Table structure for t_format_template
-- ----------------------------
DROP TABLE IF EXISTS `t_format_template`;
CREATE TABLE `t_format_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `page_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `heading_patterns` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `cover_config` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `generate_toc` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏍煎紡妯℃澘琛�' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_format_template
-- ----------------------------
INSERT INTO `t_format_template` VALUES (42, 1, '桂林信息科技学院模板', '{\"paper\":\"A4\",\"margin\":{\"top\":2.5,\"bottom\":2.5,\"left\":2.5,\"right\":2.5},\"header\":{\"height\":1.5,\"text\":\"\"},\"footer\":{\"pageNumber\":\"center\"}}', '2026-08-05 15:30:41', '2026-08-05 15:50:14', '{\"heading1\":\"^\\\\d+\",\"heading2\":\"^\\\\d+\\\\.\\\\d+\",\"heading3\":\"^\\\\d+\\\\.\\\\d+\\\\.\\\\d+\"}', '{\"enabled\":false,\"title\":\"基于规则配置的毕业论文自动排版系统设计与实现\",\"college\":\"信息工程学院\",\"major\":\"软件工程\",\"studentName\":\"张三\",\"studentNo\":\"2020010101\",\"teacherUnit\":\"信息工程学院\",\"teacher\":\"李四\",\"teacherTitle\":\"教授\",\"topicType\":\"软件开发\",\"date\":\"2026 年 6 月 15 日\"}', 0);
INSERT INTO `t_format_template` VALUES (47, 2, 'shuffle-test-tpl', '{\"paper\":\"A4\",\"margin\":{\"top\":2.5,\"bottom\":2.5,\"left\":3,\"right\":2.5},\"header\":{\"height\":1.5,\"text\":\"\"},\"footer\":{\"pageNumber\":\"center\"}}', '2026-08-05 16:37:13', '2026-08-05 16:37:13', NULL, NULL, 0);
INSERT INTO `t_format_template` VALUES (48, 2, 'shuffle-test-tpl', '{\"paper\":\"A4\",\"margin\":{\"top\":2.5,\"bottom\":2.5,\"left\":3,\"right\":2.5},\"header\":{\"height\":1.5,\"text\":\"\"},\"footer\":{\"pageNumber\":\"center\"}}', '2026-08-05 16:37:49', '2026-08-05 16:37:49', NULL, NULL, 0);
INSERT INTO `t_format_template` VALUES (49, 2, 'shuffle-test-tpl', '{\"paper\":\"A4\",\"margin\":{\"top\":2.5,\"bottom\":2.5,\"left\":3,\"right\":2.5},\"header\":{\"height\":1.5,\"text\":\"\"},\"footer\":{\"pageNumber\":\"center\"}}', '2026-08-05 16:40:15', '2026-08-05 16:40:15', NULL, NULL, 0);
INSERT INTO `t_format_template` VALUES (50, 2, 'shuffle-test-tpl', '{\"paper\":\"A4\",\"margin\":{\"top\":2.5,\"bottom\":2.5,\"left\":3,\"right\":2.5},\"header\":{\"height\":1.5,\"text\":\"\"},\"footer\":{\"pageNumber\":\"center\"}}', '2026-08-05 16:41:51', '2026-08-05 16:41:51', NULL, NULL, 0);
INSERT INTO `t_format_template` VALUES (53, 1, 'caption_off_test', NULL, '2026-08-05 16:53:10', '2026-08-05 16:53:10', '{\"heading1\":\"^?[???????????]+?\",\"heading2\":\"^\\\\d+\\\\.\\\\d+\",\"heading3\":\"^\\\\d+\\\\.\\\\d+\\\\.\\\\d+\"}', NULL, 0);

-- ----------------------------
-- Table structure for t_paper_file
-- ----------------------------
DROP TABLE IF EXISTS `t_paper_file`;
CREATE TABLE `t_paper_file`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `stored_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `file_size` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 69 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璁烘枃鏂囦欢琛�' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_paper_file
-- ----------------------------
INSERT INTO `t_paper_file` VALUES (1, 1, 'test_paper.docx', 'upload/20260805/9c1fc89b164846a1985f7759c5413868.docx', 2415, '2026-08-05 10:35:54');
INSERT INTO `t_paper_file` VALUES (2, 1, 'test_paper.docx', 'upload/20260805/42b608abea0d48a1a58dd5c28261ce18.docx', 2415, '2026-08-05 10:42:13');
INSERT INTO `t_paper_file` VALUES (3, 1, 'test_paper.docx', 'upload/20260805/a7c4785e00aa40cfaead9cbea03d064e.docx', 2415, '2026-08-05 10:45:22');
INSERT INTO `t_paper_file` VALUES (4, 1, 'test_paper.docx', 'upload/20260805/cdfd3b6e163f4b7687d8fc1ae93a3f94.docx', 2415, '2026-08-05 10:57:58');
INSERT INTO `t_paper_file` VALUES (5, 1, 'test_paper.docx', 'upload/20260805/f406280deec44e0fab7465689b51c8c3.docx', 2415, '2026-08-05 11:08:51');
INSERT INTO `t_paper_file` VALUES (6, 1, 'input_number.docx', 'upload/20260805/e81cc1474db04ac8a812475df1812ee7.docx', 1061, '2026-08-05 11:10:04');
INSERT INTO `t_paper_file` VALUES (7, 1, 'input_cn.docx', 'upload/20260805/57c2de7cefd94e639d3921979e6e888d.docx', 1053, '2026-08-05 11:10:05');
INSERT INTO `t_paper_file` VALUES (8, 1, 'input_number.docx', 'upload/20260805/87e551f2e8b4499eb7c504a1bd955942.docx', 1061, '2026-08-05 11:14:14');
INSERT INTO `t_paper_file` VALUES (9, 1, 'input_cn.docx', 'upload/20260805/41bccb4130ff4dd4a013fffaff2a33b0.docx', 1053, '2026-08-05 11:14:16');
INSERT INTO `t_paper_file` VALUES (10, 1, 'input_number.docx', 'upload/20260805/c1a9ea88f86441b0a46c195261fcdeb0.docx', 1061, '2026-08-05 11:14:59');
INSERT INTO `t_paper_file` VALUES (11, 1, 'input_cn.docx', 'upload/20260805/f486074f61fc4844ae84fe20ae5ce7aa.docx', 1053, '2026-08-05 11:15:00');
INSERT INTO `t_paper_file` VALUES (12, 1, 'test_paper.docx', 'upload/20260805/4a9eb48da94d4ae2ada54f1452d42fb7.docx', 2415, '2026-08-05 11:15:08');
INSERT INTO `t_paper_file` VALUES (13, 1, 'input.docx', 'upload/20260805/9340adac9b724204a83342d5c9465037.docx', 1717, '2026-08-05 11:39:11');
INSERT INTO `t_paper_file` VALUES (14, 1, 'input.docx', 'upload/20260805/c730e06796474070847f5640aeb129b6.docx', 1717, '2026-08-05 11:41:26');
INSERT INTO `t_paper_file` VALUES (15, 1, 'input.docx', 'upload/20260805/19d0b6c6a8f847ef814d0787b02e7c22.docx', 1717, '2026-08-05 11:49:01');
INSERT INTO `t_paper_file` VALUES (16, 1, 'input.docx', 'upload/20260805/0b492b8f48cf42f3b03c47798ae06b42.docx', 1717, '2026-08-05 11:55:18');
INSERT INTO `t_paper_file` VALUES (17, 1, 'input.docx', 'upload/20260805/aeb96d12a23c40e6bf3ec4b798053354.docx', 1717, '2026-08-05 11:55:24');
INSERT INTO `t_paper_file` VALUES (18, 1, 'input.docx', 'upload/20260805/2f4d5e16cac14c9ab00777866f242479.docx', 1717, '2026-08-05 12:01:27');
INSERT INTO `t_paper_file` VALUES (19, 1, 'input.docx', 'upload/20260805/1138c56d8b594aa09efe5dfea7d365c5.docx', 1717, '2026-08-05 12:04:13');
INSERT INTO `t_paper_file` VALUES (20, 1, 'test_paper.docx', 'upload/20260805/616449683b6a40b9a5f328d7e8ab4d0b.docx', 2415, '2026-08-05 12:04:19');
INSERT INTO `t_paper_file` VALUES (21, 1, 'test_paper.docx', 'upload/20260805/3dd29ef5a427414eb2d2d60ae6703015.docx', 2415, '2026-08-05 12:06:41');
INSERT INTO `t_paper_file` VALUES (22, 1, 'input.docx', 'upload/20260805/fbda4c4dada44596a94af5a27b1c7d4a.docx', 1717, '2026-08-05 12:06:46');
INSERT INTO `t_paper_file` VALUES (23, 1, 'test_paper.docx', 'upload/20260805/7185a08956b04ca4995430dee51b140d.docx', 2415, '2026-08-05 14:26:46');
INSERT INTO `t_paper_file` VALUES (24, 1, 'test_paper.docx', 'upload/20260805/1f7a1fd7673941e2a927ca66f72b8411.docx', 2415, '2026-08-05 14:35:28');
INSERT INTO `t_paper_file` VALUES (25, 1, 'test_paper.docx', 'upload/20260805/c8097d9962b14263afcbe23722de051d.docx', 2415, '2026-08-05 14:35:41');
INSERT INTO `t_paper_file` VALUES (26, 1, 'test_paper.docx', 'upload/20260805/f6960d77e03f4644a5d4f1affb79b923.docx', 2415, '2026-08-05 14:35:42');
INSERT INTO `t_paper_file` VALUES (27, 1, 'test_paper.docx', 'upload/20260805/276d870b5217472c91a8d0efc2604f43.docx', 2415, '2026-08-05 14:38:51');
INSERT INTO `t_paper_file` VALUES (28, 1, 'test_paper.docx', 'upload/20260805/9f842fc9fcd44557967f2fe6bd6cd12c.docx', 2415, '2026-08-05 14:38:53');
INSERT INTO `t_paper_file` VALUES (29, 1, 'test_paper.docx', 'upload/20260805/0731851822ea4f5b8b4e59eeab0c47ec.docx', 2415, '2026-08-05 14:39:11');
INSERT INTO `t_paper_file` VALUES (30, 1, 'test_paper.docx', 'upload/20260805/61bb08af6b284631904ab5432c96bc2b.docx', 2415, '2026-08-05 14:39:12');
INSERT INTO `t_paper_file` VALUES (31, 1, 'test_paper.docx', 'upload/20260805/6cef1ee53af94dc6a3c202c1946fd0de.docx', 2415, '2026-08-05 14:39:19');
INSERT INTO `t_paper_file` VALUES (32, 1, 'input.docx', 'upload/20260805/52c7126b17e8447bb243893070dfb359.docx', 1717, '2026-08-05 14:39:21');
INSERT INTO `t_paper_file` VALUES (33, 1, 'test_paper.docx', 'upload/20260805/8591279741784fdfad4bf3494e46f37f.docx', 2415, '2026-08-05 14:48:37');
INSERT INTO `t_paper_file` VALUES (34, 1, 'test_paper.docx', 'upload/20260805/aed26cd6e2214d27a20f4c240e57acc7.docx', 2415, '2026-08-05 14:48:39');
INSERT INTO `t_paper_file` VALUES (35, 1, 'input.docx', 'upload/20260805/513c66edb7394c60b1bedbad3d6b45b7.docx', 1717, '2026-08-05 14:49:11');
INSERT INTO `t_paper_file` VALUES (36, 1, 'test_paper.docx', 'upload/20260805/84b45f4437744d649ab91ed77f028185.docx', 2415, '2026-08-05 14:49:13');
INSERT INTO `t_paper_file` VALUES (37, 1, 'test_paper.docx', 'upload/20260805/e063a3b899184b2f800eb52ab0652b87.docx', 2415, '2026-08-05 14:49:14');
INSERT INTO `t_paper_file` VALUES (38, 1, 'test_paper.docx', 'upload/20260805/d3ec1cbc2cfc472c939935685a5434ef.docx', 2415, '2026-08-05 14:49:25');
INSERT INTO `t_paper_file` VALUES (39, 1, '2253400404廖彦伊.docx', 'upload/20260805/ee04d51826e942d5a4a82f6fd9316911.docx', 115870, '2026-08-05 14:56:02');
INSERT INTO `t_paper_file` VALUES (40, 1, '2253400404 廖彦伊.docx', 'upload/20260805/8e47e3a4c10c4faf9b1edbbb0b84b3b3.docx', 1370299, '2026-08-05 14:59:55');
INSERT INTO `t_paper_file` VALUES (41, 1, '2253400404廖彦伊.docx', 'upload/20260805/d5efeb805bef4ba8a9febbc741fc9a59.docx', 116095, '2026-08-05 15:06:03');
INSERT INTO `t_paper_file` VALUES (42, 1, 'input.docx', 'upload/20260805/3e4527b4671e41f482179cac83d4eb83.docx', 1717, '2026-08-05 15:06:12');
INSERT INTO `t_paper_file` VALUES (43, 1, 'test_paper.docx', 'upload/20260805/44e9c301d56f4bff860bd97fb998c400.docx', 2415, '2026-08-05 15:06:38');
INSERT INTO `t_paper_file` VALUES (44, 1, 'test_paper_num.docx', 'upload/20260805/2f0c959ed98e4147b0d3ec1f67391ad1.docx', 2471, '2026-08-05 15:18:28');
INSERT INTO `t_paper_file` VALUES (45, 1, 'test_paper_num.docx', 'upload/20260805/787672e870324bc18212bb226ed25cee.docx', 2471, '2026-08-05 15:21:33');
INSERT INTO `t_paper_file` VALUES (46, 1, 'test_paper_num.docx', 'upload/20260805/7019cc58a3d94accb620a6c35da3106f.docx', 2471, '2026-08-05 15:22:25');
INSERT INTO `t_paper_file` VALUES (47, 1, 'test_paper.docx', 'upload/20260805/404e33abad2b47d29101345fd7158494.docx', 2415, '2026-08-05 15:22:33');
INSERT INTO `t_paper_file` VALUES (48, 1, 'input.docx', 'upload/20260805/97e9e79f3d4f42e3aa2bcf47692b431c.docx', 1717, '2026-08-05 15:22:35');
INSERT INTO `t_paper_file` VALUES (49, 1, 'test_paper_num.docx', 'upload/20260805/4f227a9e77c3458790794fb3c8250dab.docx', 2486, '2026-08-05 15:30:26');
INSERT INTO `t_paper_file` VALUES (50, 1, 'test_paper.docx', 'upload/20260805/570ac4ada96b4747b1f561497c9300c0.docx', 2415, '2026-08-05 15:30:39');
INSERT INTO `t_paper_file` VALUES (51, 1, 'input.docx', 'upload/20260805/082e3857ae274b5abe9b8e3913ae482e.docx', 1717, '2026-08-05 15:30:41');
INSERT INTO `t_paper_file` VALUES (52, 1, '2253400404廖彦伊.docx', 'upload/20260805/435e761216594b8b9bfa856b16fe92c3.docx', 116396, '2026-08-05 15:31:24');
INSERT INTO `t_paper_file` VALUES (53, 1, '2253400404廖彦伊.docx', 'upload/20260805/765a7d2dab95474e801bfc9b88a85734.docx', 116396, '2026-08-05 15:32:56');
INSERT INTO `t_paper_file` VALUES (54, 1, 'test_paper_num.docx', 'upload/20260805/cda7556dac5a4c478be67640d681e907.docx', 2486, '2026-08-05 15:39:36');
INSERT INTO `t_paper_file` VALUES (55, 1, '2253400404廖彦伊.docx', 'upload/20260805/fc6342187c4943ff9b32b2d1721e9b50.docx', 116396, '2026-08-05 15:42:56');
INSERT INTO `t_paper_file` VALUES (56, 1, '2253400404廖彦伊.docx', 'upload/20260805/d3f9a6ae412a4c49899ff0dff225a274.docx', 116396, '2026-08-05 15:50:23');
INSERT INTO `t_paper_file` VALUES (57, 1, 'test_paper_num.docx', 'upload/20260805/5847d264d2e74d0391611ec9320416e1.docx', 2486, '2026-08-05 16:00:15');
INSERT INTO `t_paper_file` VALUES (58, 1, 'test_paper.docx', 'upload/20260805/6f8444c732bb43e4ab53d398336734e6.docx', 2415, '2026-08-05 16:00:37');
INSERT INTO `t_paper_file` VALUES (59, 1, '2253400404廖彦伊.docx', 'upload/20260805/721411c4b6d5471cb85cba73f25a14c5.docx', 116396, '2026-08-05 16:03:27');
INSERT INTO `t_paper_file` VALUES (60, 1, 'test_paper.docx', 'upload/20260805/3ec584ab31e04038be87bfff52466385.docx', 2415, '2026-08-05 16:24:04');
INSERT INTO `t_paper_file` VALUES (61, 1, '2253400404廖彦伊.docx', 'upload/20260805/9dff8ef0677646c3b102ab3157db0679.docx', 116396, '2026-08-05 16:24:47');
INSERT INTO `t_paper_file` VALUES (62, 1, '下面是一篇随机生成的本科论文示例.docx', 'upload/20260805/aa33b77e3d7441a5b113f65350d4a682.docx', 21640, '2026-08-05 16:25:13');
INSERT INTO `t_paper_file` VALUES (63, 2, 'shuffled.docx', 'upload/20260805/e6d6c9cfb8624f4988c53e676483993e.docx', 19672, '2026-08-05 16:37:13');
INSERT INTO `t_paper_file` VALUES (64, 2, 'shuffled.docx', 'upload/20260805/c325186fff8e46a8832d821659103e24.docx', 20040, '2026-08-05 16:37:49');
INSERT INTO `t_paper_file` VALUES (65, 2, 'shuffled.docx', 'upload/20260805/28fa7a8d6c8546379a3b65c07fd86283.docx', 20093, '2026-08-05 16:40:15');
INSERT INTO `t_paper_file` VALUES (66, 2, 'shuffled.docx', 'upload/20260805/b17ccb8669d84f6d9b87103caa0e7ed5.docx', 20076, '2026-08-05 16:41:51');
INSERT INTO `t_paper_file` VALUES (67, 1, 'shuffle_result.docx', 'upload/20260805/8a5baacba5db4ff49b7a230cd7b5ef7c.docx', 18304, '2026-08-05 16:53:33');
INSERT INTO `t_paper_file` VALUES (68, 1, 'aa33b77e3d7441a5b113f65350d4a682.docx', 'upload/20260805/2522e61b8ab24b82ad8af89de2029416.docx', 21640, '2026-08-05 16:54:33');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛琛�' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'testuser', '4a6c2f345b6ee4966c7a689c5c689846', 'testuser', '2026-08-05 10:35:54');
INSERT INTO `t_user` VALUES (2, 'shuffletest', 'c7456661905ec383f76ea69ecfc7ed62', 'shuffletest', '2026-08-05 16:37:13');
INSERT INTO `t_user` VALUES (3, 'swtest1', 'c03e618b66eb587670aa60aaf6a9f41d', 'swtest1', '2026-08-07 16:30:30');

SET FOREIGN_KEY_CHECKS = 1;
