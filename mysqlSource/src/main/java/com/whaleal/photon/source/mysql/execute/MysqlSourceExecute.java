package com.whaleal.photon.source.mysql.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.source.mysql.sourcesplit.MysqlSourceSplitRange;
import com.whaleal.photon.source.mysql.task.MysqlSourceTask;
import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.thread.SysPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlSourceExecute extends AbstractSourceExecute {
    /**
     * jdbcTemplate链接器
     */
    private JdbcTemplate jdbcTemplate;
    /**
     * SourceTaskInfo队列Map
     * k为程序名+批次号
     * v为SourceTaskInfo队列
     */
    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public MysqlSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        procSourceTask.put(getProcNameAndBatchNo(), taskMetadataQueue);
        jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndSourceDsName());
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
                dbTableNameMap.put(dbTable, dbTable);
            }
        }
        Log.info(getProcNameAndBatchNo() + ",全量同步的表列表:" + dbTableNameMap);
    }

    @Override
    public void splitDbTable() {
        createSourceTask("");
        dbTableNameMap = new ConcurrentHashMap<>();
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceTask(String dbTableName) {
        List<Range> list = new ArrayList<>();
        try {
            list = MysqlSourceSplitRange.doSplit(programInfo);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
        for (Range splitRange : list) {
            SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 1);
            SourceTaskInfo taskMetadata = new SourceTaskInfo(splitRange, splitRange.getDbTableName(), sourceDsName);
            pushTaskMeta(getProcNameAndBatchNo(), taskMetadata);
            SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), -1);
        }
    }


    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0) > 10) {
                            TimeUnit.SECONDS.sleep(10);
                        }
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new MysqlSourceTask(taskMetadata, programInfo));
                        } else {
                            int sourceActiveThreadNum = SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0);
                            int sysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 0);
                            /**
                             *   剩余任务队列==0
                             *   活跃的source线程数==0
                             *   活跃的sys线程数==0
                             *   isGetAllDbTable是否已经获取到全部的表=true
                             *   dbTableNameMap全部的表是否都进行了切表操作
                             *   以上条件作为关闭此线程的条件
                             */
                            boolean isOver = taskMetadataQueue.size() == 0 && sourceActiveThreadNum == 0
                                    && isGetAllDbTable && dbTableNameMap.size() == 0 && sysActiveThreadNum == 0;
                            if (isOver) {
                                TimeUnit.SECONDS.sleep(10);
                                //dcl检查
                                int sourceActiveThreadNum2 = SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0);
                                int sysActiveThreadNum2 = SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 0);
                                boolean isOver2 = taskMetadataQueue.size() == 0 && sourceActiveThreadNum2 == 0
                                        && isGetAllDbTable && dbTableNameMap.size() == 0 && sysActiveThreadNum2 == 0;
                                if (isOver2) {
                                    break;
                                }
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                    }
                }
            }
        };
        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);
    }


    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procNameAndBatchNo).add(taskMetadata);
    }

    /**
     * 获取表结构为
     *
     * @return
     */
    public Map<String, String> getTableStructure() {
        Map<String, String> tableStructureMap = new HashMap<>();
        for (String dbTableName : dbTableNameSet) {
            try {
                Map<String, Object> map = jdbcTemplate.queryForMap("show create table " + dbTableName);
                String tableName = dbTableName.split("\\.", 2)[1];
                String createTableSql = map.get("Create Table").toString().split("\\) ENGINE=")[0] + ")";
                createTableSql = createTableSql.replaceFirst("CREATE TABLE `" + tableName + "`", "CREATE TABLE " + dbTableName + "");
                createTableSql = createTableSql.replaceAll("CHARACTER SET \\w+ COLLATE \\w+", " ");
                tableStructureMap.put(dbTableName, createTableSql);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
        return tableStructureMap;
    }
}
