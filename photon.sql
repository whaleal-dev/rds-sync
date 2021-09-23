/*
 Navicat Premium Data Transfer

 Source Server         : 19
 Source Server Type    : MySQL
 Source Server Version : 50724
 Source Host           : 192.168.3.19:3306
 Source Schema         : photon

 Target Server Type    : MySQL
 Target Server Version : 50724
 File Encoding         : 65001

 Date: 23/09/2021 14:08:05
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for datasource
-- ----------------------------
DROP TABLE IF EXISTS `datasource`;
CREATE TABLE `datasource` (
  `id` varchar(128) NOT NULL COMMENT 'id',
  `name` varchar(128) NOT NULL COMMENT '数据源名称',
  `type` varchar(32) DEFAULT NULL COMMENT '数据源类型：Mysql，mongodb，pg，oracle，hdfs',
  `ds_database` varchar(128) DEFAULT NULL COMMENT '库名',
  `ds_schema` varchar(128) DEFAULT NULL COMMENT '模式名',
  `username` varchar(128) DEFAULT NULL COMMENT '账号',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `url` varchar(512) DEFAULT NULL COMMENT '链接字符串',
  `ip` varchar(32) DEFAULT NULL COMMENT 'ip',
  `port` varchar(8) DEFAULT NULL COMMENT '端口',
  `status` tinyint(1) DEFAULT NULL COMMENT '状态：1:启动 0:禁用',
  `create_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(128) DEFAULT NULL COMMENT '标记',
  `update_user_name` varchar(128) DEFAULT NULL,
  `ds_option` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for program
-- ----------------------------
DROP TABLE IF EXISTS `program`;
CREATE TABLE `program` (
  `id` varchar(128) NOT NULL,
  `task_name` varchar(128) DEFAULT NULL COMMENT '任务名称',
  `proc_name` varchar(128) DEFAULT NULL COMMENT '程序名称',
  `team_name` varchar(128) DEFAULT NULL COMMENT '团队名称',
  `source_ds_name` varchar(128) DEFAULT NULL COMMENT '源端数据源名称',
  `target_ds_name` varchar(128) DEFAULT NULL COMMENT '目标数据源名称',
  `sync_mode` varchar(10) DEFAULT NULL COMMENT '同步模式',
  `sync_parallel` tinyint(1) DEFAULT NULL COMMENT '多个源数据源是否并行执行',
  `db_table_white` varchar(100) DEFAULT NULL COMMENT '表过滤正则',
  `filter_ddl` tinyint(1) DEFAULT NULL COMMENT '是否同步DDL',
  `collection_exist_drop` tinyint(1) DEFAULT NULL COMMENT '已经存在的表是否删除',
  `create_index` tinyint(1) DEFAULT NULL COMMENT '是否创建索引',
  `target_thread_num` tinyint(2) DEFAULT NULL COMMENT 'target任务线程数',
  `source_thread_num` tinyint(2) DEFAULT NULL COMMENT 'source任务线程数',
  `cache_size` int(3) DEFAULT NULL COMMENT '缓存区个数',
  `cache_num` int(3) DEFAULT NULL COMMENT '每个缓存区缓存批次数量',
  `data_batch_size` int(3) DEFAULT NULL COMMENT '每个批次数据的大小',
  `start_increment_time` int(11) DEFAULT NULL COMMENT '增量同步时，设置增量开始同步的时间。时间戳格式，单位s',
  `increment_parse_thread_num` tinyint(2) DEFAULT NULL COMMENT '在增量中每个数据源解析数据的线程',
  `split_pk` varchar(128) DEFAULT NULL COMMENT '切表字段',
  `fetch_size` int(11) DEFAULT NULL COMMENT 'JDBC缓存记录数',
  `advice_number` int(11) DEFAULT NULL COMMENT '切分数',
  `create_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for task_trigger
-- ----------------------------
DROP TABLE IF EXISTS `task_trigger`;
CREATE TABLE `task_trigger` (
  `id` varchar(255) NOT NULL,
  `task_name` varchar(128) NOT NULL COMMENT '任务名',
  `proc_name` varchar(128) NOT NULL COMMENT '程序名',
  `batch_no` varchar(32) DEFAULT NULL COMMENT '批次号',
  `batch_type` varchar(16) DEFAULT NULL COMMENT '批次类型',
  `is_turnover` varchar(100) DEFAULT '1' COMMENT '是否周转',
  `broker_id` varchar(16) DEFAULT NULL COMMENT '执行主机id',
  `broker_ip` varchar(128) DEFAULT NULL COMMENT '执行主机ip',
  `create_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `start_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务开始执行时间',
  `complete_dt` timestamp NULL DEFAULT NULL COMMENT '任务完成时间',
  `last_upd_dt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `message` longtext COMMENT '执行信息',
  `state` varchar(20) DEFAULT NULL COMMENT '执行状态 new start running stop',
  `team_name` varchar(32) DEFAULT NULL COMMENT '团队名',
  `is_new` varchar(32) DEFAULT NULL COMMENT 'isNew',
  `priority` int(11) DEFAULT '9' COMMENT '优先级',
  `duration` int(11) DEFAULT '0' COMMENT '间隔',
  `work_group` varchar(64) DEFAULT NULL COMMENT '工作组',
  `member_name` varchar(128) DEFAULT NULL COMMENT '会员名',
  `run_state` int(11) DEFAULT '0' COMMENT '运行状态',
  `is_valid` int(11) DEFAULT '0' COMMENT '是否验证',
  `rtn_code` varchar(5) DEFAULT NULL COMMENT 'rtnCode',
  `actual_redo_num` int(11) DEFAULT '0' COMMENT '实际重做次数',
  `avg_duration` bigint(20) DEFAULT '0' COMMENT '平均持续时间',
  `parent_code` varchar(128) DEFAULT NULL COMMENT 'parentCode',
  `initiated_team` varchar(32) DEFAULT NULL COMMENT '发起团队',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


