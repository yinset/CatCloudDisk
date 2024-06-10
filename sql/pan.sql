/*
 Navicat Premium Data Transfer

 Source Server         : 阿里云
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : bgpohf.top:3306
 Source Schema         : pan

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 04/06/2024 23:51:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `file_id` int(0) NOT NULL AUTO_INCREMENT,
  `user_id` int(0) NULL DEFAULT 0,
  `file_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `file_size` bigint(0) NULL DEFAULT NULL,
  `file_super_id` int(0) NULL DEFAULT -1 COMMENT '默认-1，为根目录，若是其他数字则表示在该file_id文件夹内',
  `file_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `folder_type` int(0) NULL DEFAULT -1 COMMENT '-1为文件 1为目录',
  `del_flag` int(0) NULL DEFAULT -1,
  `del_indirect_flag` int(0) NULL DEFAULT -1,
  `hard_del_flag` int(0) NULL DEFAULT -1 COMMENT '1为被彻底删除，不再显示，n天后未被还原（未被修正为-1）则通过计划任务清除数据库条目',
  `recover_flag` int(0) NULL DEFAULT -1 COMMENT '当文件的父文件夹被删除时恢复文件，此标记为1可以让父文件夹显示在个人文件中。解决因父文件夹被删文件还原后不显示的问题',
  `file_uid` bigint(0) NULL DEFAULT 0 COMMENT '使用时间戳标记每一个文件，用于前端上传验证是否完成',
  `relative_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '相对目录',
  `file_md5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT 'MD5',
  `cycle_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '文件在回收站的路径 默认为空',
  `delete_time` datetime(0) NULL DEFAULT NULL COMMENT '删除时间点',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28664 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `user_id` int(0) NOT NULL AUTO_INCREMENT,
  `nick_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `email` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `password` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `avatar_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT NULL,
  `total_space` bigint(0) NULL DEFAULT 2155374184,
  `use_space` bigint(0) NULL DEFAULT 0,
  `ban` int(0) NULL DEFAULT -1 COMMENT '1为账号禁用默认-1',
  `birthday` date NULL DEFAULT NULL,
  `is_admin` tinyint(0) NULL DEFAULT -1 COMMENT '1为管理默认-1',
  PRIMARY KEY (`user_id`, `email`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Procedure structure for cycle_auto_delete
-- ----------------------------
DROP PROCEDURE IF EXISTS `cycle_auto_delete`;
delimiter ;;
CREATE PROCEDURE `cycle_auto_delete`()
BEGIN
	delete from file_info where delete_time < (now() - interval 90 day) and recover_flag = -1;
	update file_info 
	set del_flag = -1,del_indirect_flag = -1,recover_flag = -1,delete_time = NULL,hard_del_flag=-1,cycle_path=null 
	where delete_time < (now() - interval 90 day) and recover_flag = 1;
END
;;
delimiter ;

-- ----------------------------
-- Event structure for cycle_auto_delete
-- ----------------------------
DROP EVENT IF EXISTS `cycle_auto_delete`;
delimiter ;;
CREATE EVENT `cycle_auto_delete`
ON SCHEDULE
EVERY '1' DAY STARTS '2024-05-04 17:43:48'
DO call cycle_auto_delete()
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
