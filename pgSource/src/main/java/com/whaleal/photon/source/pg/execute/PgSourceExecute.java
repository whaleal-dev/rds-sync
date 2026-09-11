package com.whaleal.photon.source.pg.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.source.pg.sourcesplit.PgSourceSplitRange;
import com.whaleal.photon.source.pg.task.PgSourceTask;
import com.whaleal.photon.core.thread.SourceTaskPoolManager;
import com.whaleal.photon.core.thread.SysPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: PgSource类
 */

public class PgSourceExecute extends AbstractSourceExecute {
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

    public PgSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        procSourceTask.put(getProcNameAndBatchNo(), taskMetadataQueue);
        this.jdbcTemplate = PgServerConnection.getJdbcTemplate(getProcNameAndBatchNoAndSourceDsName());
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
        String sql = "select  * from information_schema.TABLES where table_type='BASE TABLE' and concat(table_schema,'.',table_name)  ~ ? ";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql, dbTableWhite);
        for (Map<String, Object> dbTableNameMapTemp : dbTableMapList) {
            String dbSchemaName = dbTableNameMapTemp.get("table_schema").toString();
            String tableName = dbTableNameMapTemp.get("table_name").toString();
            String dbTable = dbSchemaName + "." + tableName;
            dbTableNameMap.put(dbTable, dbTable);
            dbTableNameSet.add(dbTable);
        }
        Log.info(getProcNameAndBatchNo() + ",全量同步的表列表:" + dbTableNameMap);
    }

    @Override
    public void splitDbTable() {
        Iterator<Map.Entry<String, String>> mapIterator = dbTableNameMap.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceTask(next.getValue());
            dbTableNameMap.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceTask(String dbTableName) {
        PgSourceSplitRange source = new PgSourceSplitRange(sourceDsName, proName, batchNo);
        List<Range> rangeList = source.getRangeList(dbTableName);
        for (Range range : rangeList) {
            SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 1);
            SourceTaskInfo sourceTaskInfo = new SourceTaskInfo();
            sourceTaskInfo.setSourceDsName(sourceDsName);
            sourceTaskInfo.setDbTableName(dbTableName);
            sourceTaskInfo.setRange(range);
            pushTaskMeta(getProcNameAndBatchNo(), sourceTaskInfo);
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
                            SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new PgSourceTask(taskMetadata, programInfo));
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
                        break;
                    }

                }
            }
        };
        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);
    }

    @Override
    public void executeQueryTask() {
        SourceTaskInfo taskMetadata = new SourceTaskInfo();
        taskMetadata.setSourceDsName(sourceDsName);
        Range range = new Range();
        range.setSql(programInfo.getQuerySql());
        taskMetadata.setDbTableName(programInfo.getDbTableName());
        range.setDbTableName(programInfo.getDbTableName());
        taskMetadata.setRange(range);
        SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new PgSourceTask(taskMetadata, programInfo));
    }
    /**
     * 提交TaskInfo到任务队列中
     *
     * @param procNameAndBatchNo
     * @param sourceTaskInfo
     */
    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo sourceTaskInfo) {
        procSourceTask.get(procNameAndBatchNo).add(sourceTaskInfo);
    }
}
