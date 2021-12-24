package com.whaleal.photon.source.mysql.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;

import com.whaleal.photon.core.thread.SourceTaskPoolManager;
import com.whaleal.photon.core.thread.SysPoolManager;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.source.mysql.task.MysqlSourceTask;

import com.whaleal.photon.common.util.Log;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlSourceExecute extends AbstractSourceExecute {
    /**
     * jdbcTemplate链接器
     */
    private final JdbcTemplate jdbcTemplate;


    public MysqlSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        jdbcTemplate = MySqlConnection.getJdbcTemplate(programInfo.getSourceDsName());
    }

    @Override
    public void start() {
        // 获取数据源的全部库表
        getAllDbTables();
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        splitDbTable();
    }

    @Override
    public void getAllDbTables() {
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from information_schema.TABLES");
        for (Map dbTableMap : dbTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            //忽略 mysql 系统表
            if (dbName.equalsIgnoreCase("mysql") || dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTableNameSet.add(dbTable);
                // 计算当前表的数量
                long documentCount = jdbcTemplate.queryForObject("select count(1) from " + dbTable, Long.class);
                // 更新总数量
                memoryCache.allDocCount += documentCount;
            }
        }
        Log.info("程序:" + proName + ",全量同步的表列表:" + dbTableNameSet);
    }

    @Override
    public void splitDbTable() {
        Iterator<String> iterator = dbTableNameSet.iterator();
        while (iterator.hasNext()) {
            String dbTableName = iterator.next();
            createSourceTask(dbTableName);
        }
        this.dbTableNameSet = new HashSet<>();
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceTask(String dbTableName) {
//        List<Range> list = new ArrayList<>();
//        try {
//            list = MysqlSourceSplitRange.
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.error(e.getMessage());
//        }
//        for (Range splitRange : list) {
//            SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 1);
//            SourceTaskInfo taskMetadata = new SourceTaskInfo(splitRange, splitRange.getDbTableName(), sourceDsName);
//            pushTaskMeta(getProcNameAndBatchNo(), taskMetadata);
//            SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), -1);
//        }
    }


    @Override
    public void submitSourceTask() {
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        if (SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0) > 10) {
//                            TimeUnit.SECONDS.sleep(10);
//                        }
//                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
//                        if (taskMetadata != null) {
//                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata, programInfo));
//                        } else {
//                            int sourceActiveThreadNum = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
//                            int sysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(proName, 0);
//                            /**
//                             *   剩余任务队列==0
//                             *   活跃的source线程数==0
//                             *   活跃的sys线程数==0
//                             *   isGetAllDbTable是否已经获取到全部的表=true
//                             *   dbTableNameMap全部的表是否都进行了切表操作
//                             *   以上条件作为关闭此线程的条件
//                             */
//                            boolean isOver = taskMetadataQueue.size() == 0 && sourceActiveThreadNum == 0
//                                    && isGetAllDbTable && dbTableNameSet.size() == 0 && sysActiveThreadNum == 0;
//                            if (isOver) {
//                                TimeUnit.SECONDS.sleep(10);
//                                //dcl检查
//                                int sourceActiveThreadNum2 = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
//                                int sysActiveThreadNum2 = SysPoolManager.setSysActiveThreadNum(proName, 0);
//                                boolean isOver2 = taskMetadataQueue.size() == 0 && sourceActiveThreadNum2 == 0
//                                        && isGetAllDbTable && dbTableNameSet.size() == 0 && sysActiveThreadNum2 == 0;
//                                if (isOver2) {
//                                    break;
//                                }
//                            }
//                            TimeUnit.SECONDS.sleep(2);
//                        }
//                    } catch (InterruptedException e) {
//                        Log.error(e.getMessage());
//                    }
//                }
//            }
//        };
//        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // 此线程不需要停止 让它一直打乱SourceTaskInfo的顺序吧
                while (true) {
                    try {
                        if (SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0) > programInfo.getSourceThreadNum() * 2) {
                            // 乱序操作
                            if (!programInfo.isParallelSynchronizationMultipleTables()) {
                                // 让SourceTaskInfo乱序 。相邻的两个交换,然后再加入队列中
                                SourceTaskInfo taskMetadataTemp1 = taskMetadataQueue.poll();
                                SourceTaskInfo taskMetadataTemp2 = taskMetadataQueue.poll();
                                // 判断两个是否同时是否为空
                                if (taskMetadataTemp1 != null && taskMetadataTemp2 != null) {
                                    taskMetadataQueue.add(taskMetadataTemp2);
                                    taskMetadataQueue.add(taskMetadataTemp1);
                                } else {
                                    // 数据再塞回去
                                    if (taskMetadataTemp1 != null) {
                                        taskMetadataQueue.add(taskMetadataTemp1);
                                    }
                                    if (taskMetadataTemp2 != null) {
                                        taskMetadataQueue.add(taskMetadataTemp2);
                                    }
                                }
                            }
                            // 就是要睡眠了
                            TimeUnit.SECONDS.sleep(2);
                            continue;
                        }
                        // 拉取任务信息
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            //把任务提交到SourceTaskPoolManager
                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata, programInfo));
                        } else {
                            int sourceActiveThreadNum = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
                            int sysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(proName, 0);
                            /**
                             *   剩余任务队列==0
                             *   活跃的source线程数==0
                             *   活跃的sys线程数==0
                             *   isGetAllDbTable是否已经获取到全部的表=true
                             *   以上条件作为关闭此线程的条件
                             */
                            boolean isOver = taskMetadataQueue.size() == 0 && sourceActiveThreadNum == 0
                                    && isGetAllDbTable && sysActiveThreadNum == 0;
                            if (isOver) {
                                TimeUnit.SECONDS.sleep(10);
                                // dcl检查
                                int sourceActiveThreadNum2 = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
                                int sysActiveThreadNum2 = SysPoolManager.setSysActiveThreadNum(proName, 0);
                                boolean isOver2 = taskMetadataQueue.size() == 0 && sourceActiveThreadNum2 == 0
                                        && isGetAllDbTable && sysActiveThreadNum2 == 0;
                                if (isOver2) {
                                    break;
                                }
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }
                        // 就是要睡眠了
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Log.error("程序:" + proName + ",创建SourceTask时发生错误,报错信息:" + e.getMessage());
                        // 出现强制关闭InterruptedException的情况下，关闭此线程
                        break;
                    }
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    @Override
    public void executeQueryTask() {

    }


//    /**
//     * 获取表结构为
//     *
//     * @return
//     */
//    public Map<String, String> getTableStructure() {
//        Map<String, String> tableStructureMap = new HashMap<>();
//        for (String dbTableName : dbTableNameSet) {
//            try {
//                Map<String, Object> map = jdbcTemplate.queryForMap("show create table " + dbTableName);
//                String tableName = dbTableName.split("\\.", 2)[1];
//                String createTableSql = map.get("Create Table").toString().split("\\) ENGINE=")[0] + ")";
//                createTableSql = createTableSql.replaceFirst("CREATE TABLE `" + tableName + "`", "CREATE TABLE " + dbTableName + "");
//                createTableSql = createTableSql.replaceAll("CHARACTER SET \\w+ COLLATE \\w+", " ");
//                tableStructureMap.put(dbTableName, createTableSql);
//            } catch (Exception e) {
//                Log.error(e.getMessage());
//            }
//        }
//        return tableStructureMap;
//    }


}
