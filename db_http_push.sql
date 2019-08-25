/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MariaDB
 Source Server Version : 100306
 Source Host           : localhost:3306
 Source Schema         : db_http_push

 Target Server Type    : MariaDB
 Target Server Version : 100306
 File Encoding         : 65001

 Date: 25/08/2019 13:46:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_push_config
-- ----------------------------
DROP TABLE IF EXISTS `tb_push_config`;
CREATE TABLE `tb_push_config`  (
  `push_config_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `app_id` int(11) NOT NULL,
  `next_task_interval` int(255) NOT NULL COMMENT '时间间隔，ms',
  `least_retry_times` int(11) NOT NULL COMMENT '至少重试次数',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '推送地址',
  `capacity` int(255) NOT NULL COMMENT '批量推送满足条件',
  `push_type` int(255) NOT NULL COMMENT '推送类型，类型相同的使用同一个任务',
  `first_delayed_ms` int(255) NOT NULL COMMENT '数据库扫描，第一次延迟ms后执行',
  `delayed_cycle_ms` int(255) NOT NULL COMMENT '数据库扫描，周期ms后执行',
  `enabled` int(2) NOT NULL COMMENT '0：启用，1：禁用',
  PRIMARY KEY (`push_config_id`) USING BTREE,
  INDEX `idx_appId`(`app_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tb_push_record
-- ----------------------------
DROP TABLE IF EXISTS `tb_push_record`;
CREATE TABLE `tb_push_record`  (
  `msg_id` char(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `create_time` datetime(0) NOT NULL,
  `last_push_time` datetime(0) NOT NULL COMMENT '上次推送时间',
  `push_times` tinyint(1) NOT NULL DEFAULT 0 COMMENT '推送次数',
  `least_retry_times` tinyint(1) NOT NULL COMMENT '允许最大推送次数',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '推送地址',
  `app_id` int(6) NOT NULL COMMENT '推送的appId',
  `push_data` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '推送数据',
  `push_state` tinyint(2) NOT NULL COMMENT '推送状态\r\nPUSH_SUCCESS(0),推送成功\r\nRESPONSE_DATA_ERROR(1),响应数据错误\r\nREMOTE_CALL_FAILED(2),远程调用失败\r\nLOCAL_ERROR(3),本地错误\r\nRETRY_LIMITED(4),重试限制\r\nPUSH_WAIT(5);等待推送',
  `queue_index` tinyint(2) NULL DEFAULT NULL COMMENT '队列下标',
  `push_service_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '服务实例id',
  `push_type` tinyint(2) NOT NULL COMMENT '推送类型',
  `data_time` date NOT NULL COMMENT '推送时间',
  PRIMARY KEY (`msg_id`, `data_time`) USING BTREE,
  INDEX `idx_appId_serviceId`(`app_id`, `push_service_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for tb_push_record_no_config
-- ----------------------------
DROP TABLE IF EXISTS `tb_push_record_no_config`;
CREATE TABLE `tb_push_record_no_config`  (
  `msg_id` char(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `create_time` datetime(0) NOT NULL,
  `last_push_time` datetime(0) NOT NULL,
  `push_times` int(11) NOT NULL DEFAULT 0,
  `least_retry_times` int(11) NULL DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `app_id` int(11) NOT NULL,
  `push_data` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `push_status` char(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `push_state` int(2) NOT NULL,
  `queue_index` int(255) NULL DEFAULT NULL,
  `push_service_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `push_type` int(255) NULL DEFAULT NULL,
  `data_time` datetime(0) NOT NULL DEFAULT current_timestamp() ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`msg_id`, `data_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
