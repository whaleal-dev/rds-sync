## QuickStart

## 启动步骤

#### 1.下载Photon

直接下载，访问 ，会列出所有历史的发布版本包的下载。

#### 2.解压缩

```
mkdir /tmp/photon
tar zxvf photon.tar.gz  -C /tmp/photon
```

#### 3.配置文件修改

同级目录找到photonT.properties文件,配置程序任务数据源

```
vi photonT.properties
```

```
url=jdbc:mysql://ip:3306/photon?useUnicode=true&characterEncoding=utf-8
userName=root
password=password
```

#### 4.准备测试表

打开photon.sql文件，数据导入到mysql数据库中 打开datasource表，新增记录 INSERT
INTO `` (`id`, `name`, `type`, `ds_database`, `ds_schema`, `username`, `password`, `url`, `ip`, `port`, `status`, `create_dt`, `update_dt`, `remark`, `update_user_name`, `ds_option`) VALUES ('testhadoop', 'hadoop1', 'hadoop', NULL, NULL, NULL, NULL, 'hdfs://hadoop1:9000', NULL, NULL, NULL, '2021-09-22 16:58:22', '2021-09-22 19:36:11', NULL, NULL, NULL); INSERT INTO `` (`id`
, `name`, `type`, `ds_database`, `ds_schema`, `username`, `password`, `url`, `ip`, `port`, `status`, `create_dt`
, `update_dt`, `remark`, `update_user_name`, `ds_option`) VALUES ('1', 'mongodb1', 'mongodb', 'photon', 'photon', '
root', '123456', 'mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin&replicaset=test', '1', NULL, NULL, '
2021-08-30 13:38:21', '2021-09-16 18:32:19', NULL, NULL, NULL); INSERT
INTO `` (`id`, `name`, `type`, `ds_database`, `ds_schema`, `username`, `password`, `url`, `ip`, `port`, `status`, `create_dt`, `update_dt`, `remark`, `update_user_name`, `ds_option`) VALUES ('1111', 'mysql1111', 'mysql', 'community', NULL, 'root', 'Qq1050564479', 'jdbc:mysql://192.168.3.106:3306/community?useOldAliasMetadataBehavior=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&serverTimezone=Asia/Shanghai', '192.168.3.106', '3306', NULL, '2021-09-12 13:52:05', '2021-09-12 13:52:05', '数据源备注', NULL, NULL); INSERT INTO `` (`id`
, `name`, `type`, `ds_database`, `ds_schema`, `username`, `password`, `url`, `ip`, `port`, `status`, `create_dt`
, `update_dt`, `remark`, `update_user_name`, `ds_option`) VALUES ('4', 'oracle1', 'oracle', 'photon', 'photon', '
system', '123456', 'jdbc:oracle:thin:@192.168.3.106:19161:xe', '192.168.3.106', '3306', 1, '2021-08-30 15:16:23', '
2021-08-30 16:42:50', NULL, NULL, NULL); INSERT INTO `` (`id`, `name`, `type`, `ds_database`, `ds_schema`, `username`
, `password`, `url`, `ip`, `port`, `status`, `create_dt`, `update_dt`, `remark`, `update_user_name`, `ds_option`)
VALUES ('6', 'pg', 'pg', 'test', 'test', 'postgres', 'R00t@123', 'jdbc:postgresql://192.168.3.19:5432/test', NULL, NULL,
NULL, '2021-08-31 15:36:30', '2021-09-01 10:38:18', NULL, NULL, NULL); 打开表program，新增记录 INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('1', '
task1', 'proc1', NULL, 'mongodb1', 'mongodb2', 'all', 1, 'photon.+', 1, 1, 5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL,
NULL, '2021-09-06 14:20:27', '2021-09-15 17:48:38'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`
, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`
, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`
, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`
, `create_dt`, `update_dt`) VALUES ('10', 'task10', 'proc10', NULL, 'oracle3', 'mysqltest', 'all', 1, 'USERS.+', 1, 1,
5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, NULL, '2021-09-06 14:23:52', '2021-09-22 17:57:53'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('111', '
task111', 'proc1111', 'akmo111', 'mysql111', 'mongo111', 'all', 0, 'community.test1', 0, 0, 0, 5, 5, 0, 0, 0, 0, 0,
NULL, 500, 2, '2021-09-09 17:11:54', '2021-09-15 17:48:42'); INSERT INTO `photon`.`program` (`id`, `task_name`
, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`
, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`
, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`
, `advice_number`, `create_dt`, `update_dt`) VALUES ('11112', 'task1121', 'proc11211', 'akmo111', 'mysql111', '
mongo111', 'all', 1, 'community.test1', 1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-12 16:59:06', '
2021-09-12 16:59:06'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`
, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`
, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`
, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`
, `update_dt`) VALUES ('111212', 'task11221', 'proc112211', 'akmo111', 'mysql111', 'mongo111', 'all', 1, '
community.test1', 1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-12 16:59:37', '2021-09-12 16:59:37'); INSERT
INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('1212', '
task13', 'proc12', NULL, 'test', 'test', 'all', 1, 'community.test1', 1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 5, '
2021-09-08 02:57:14', '2021-09-08 18:29:46'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`
, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`
, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`
, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`
, `create_dt`, `update_dt`) VALUES ('1213', 'task14', 'proc20', NULL, 'test', 'test2', 'all', 1, 'community.test1', 1,
1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 5, '2021-09-08 02:57:14', '2021-09-09 14:55:58'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('12212', '
motomo', 'proc13', NULL, 'mongodb1', 'mongodb2', 'all', 1, 'photon.+', 1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 5, '
2021-09-08 02:57:14', '2021-09-08 18:14:42'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`
, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`
, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`
, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`
, `create_dt`, `update_dt`) VALUES ('122122', 'motomy', 'proc14', NULL, 'mongodb1', 'mysqltest', 'all', 1, 'ys.testys',
1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 5, '2021-09-08 02:57:14', '2021-09-15 19:57:48'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('13', '
tasktest20210915', 'protest20210915', 'akmo20210915_update', 'mongodb1', 'mongodb2', 'all', 0, 'mongo1.+', 1, 1, 1, 5,
5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 09:15:43'); INSERT INTO `photon`.`program` (`id`
, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`
, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`
, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`
, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('2', 'task1', 'proc2', NULL, 'mysqltest', '
mongodbtest', 'all', 1, 'community.col2', 1, 1, 5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, 5, '2021-09-06 14:23:52', '
2021-09-16 15:10:48'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`
, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`
, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`
, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`
, `update_dt`) VALUES ('22', 'task22', 'proc22', 'akmo20210915_update', 'mongodb2', 'mysqltest', 'all', 0, 'ys.+', 1, 1,
1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 10:43:43'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('23', '
task23', 'proc23', 'akmo20210915_update', 'oracle3', 'mongodb2', 'all', 0, 'SYSTEM.CSTEST', 1, 1, 1, 5, 5, 20, 20, 128,
0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 14:39:25'); INSERT INTO `photon`.`program` (`id`, `task_name`
, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`
, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`
, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`
, `advice_number`, `create_dt`, `update_dt`) VALUES ('24', 'task24', 'proc24', 'akmo20210915_update', 'pg', '
mongodb2', 'all', 0, 'public.student', 1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '
2021-09-16 15:04:15'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`
, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`
, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`
, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`
, `update_dt`) VALUES ('25', 'task25', 'proc25', 'akmo20210915_update', 'pg', 'mysqltest', 'all', 0, 'public.student',
1, 1, 1, 5, 5, 20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 15:19:27'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('26', '
task26', 'proc26', 'akmo20210915_update', 'oracle3', 'mysqltest', 'all', 0, 'USERS.CSTEST', 1, 1, 1, 5, 5, 20, 20, 128,
0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 15:57:02'); INSERT INTO `photon`.`program` (`id`, `task_name`
, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`
, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`
, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`
, `advice_number`, `create_dt`, `update_dt`) VALUES ('5', 'task5', 'proc5', NULL, 'pg', 'mongodb2', 'all', 1, '
public.+', 1, 1, 5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, NULL, '2021-09-06 14:23:52', '2021-09-09 15:26:03'); INSERT
INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('6', '
tesk6', 'proc6', NULL, 'mongodb1', 'mysqltest', 'all', 1, 'photon.sourceOffset', 1, 1, 5, 5, 5, 20, 20, 128, 1, 5, NULL,
NULL, NULL, '2021-09-06 14:23:52', '2021-09-09 15:26:04'); INSERT INTO `photon`.`program` (`id`, `task_name`
, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`
, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`
, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`
, `advice_number`, `create_dt`, `update_dt`) VALUES ('7', 'task7', 'proc7', NULL, 'pg', 'mysqltest', 'all', 1, '
public.+', 1, 1, 1, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, NULL, '2021-09-06 14:23:53', '2021-09-16 15:39:57'); INSERT
INTO `photon`.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('8', '
rask8', 'proc8', NULL, 'oracle3', 'mongodb2', 'all', 1, 'USERS.+', 1, 1, 5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, NULL, '
2021-09-06 14:23:53', '2021-09-09 15:26:06'); INSERT INTO `photon`.`program` (`id`, `task_name`, `proc_name`
, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`, `db_table_white`, `filter_ddl`
, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`, `cache_size`, `cache_num`
, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`
, `create_dt`, `update_dt`) VALUES ('9', 'task9', 'proc9', NULL, 'mysqltest', 'mysql1', 'all', 1, 'community.test', 1,
1, 5, 5, 5, 20, 20, 128, 1, 5, NULL, NULL, NULL, '2021-09-06 14:23:53', '2021-09-15 20:58:37'); INSERT INTO `photon`
.`program` (`id`, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`
, `sync_parallel`, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`
, `source_thread_num`, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`
, `increment_parse_thread_num`, `split_pk`, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('
protest20210915', 'task20', 'proc21', 'akmo20210915_update', 'mongodb2', 'mongodb1', 'all', 0, 'ys.+', 1, 1, 1, 5, 5,
20, 20, 128, 0, 5, NULL, 500, 2, '2021-09-15 19:30:13', '2021-09-16 09:16:23'); INSERT INTO `photon`.`program` (`id`
, `task_name`, `proc_name`, `team_name`, `source_ds_name`, `target_ds_name`, `sync_mode`, `sync_parallel`
, `db_table_white`, `filter_ddl`, `collection_exist_drop`, `create_index`, `target_thread_num`, `source_thread_num`
, `cache_size`, `cache_num`, `data_batch_size`, `start_increment_time`, `increment_parse_thread_num`, `split_pk`
, `fetch_size`, `advice_number`, `create_dt`, `update_dt`) VALUES ('testhadoop', 'task15', 'proc15', '
akmo20210915_update', 'mongodb1', 'hadoop1', 'all', 1, 'photon.sourceOffset', 1, 1, 1, 1, 5, 20, 20, 128, 1, 5, NULL,
500, NULL, '2021-09-22 16:59:34', '2021-09-22 17:30:40');

#### 5.准备启动

```
打开Execute模块，进入TestMain类，输入想执行的程序名称，点击运行

```

#### 6.查看日志

```
cat logs/log.log

错误日志会生成为error.log
```

#### 7.查看目标端的数据量，对比数据一致性。