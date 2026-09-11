package com.whaleal.photon.source.oracle.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.source.oracle.split.OracleSourceSplitRange;
import com.whaleal.photon.source.oracle.task.OracleSourceTask;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * orcle数据read执行器
 *
 * @author cs
 * @date 2021/08/30
 */
public class OracleSourceExecute extends AbstractSourceExecute {
    /**
     * SourceTaskInfo队列Map
     * k为程序名+批次号
     * v为SourceTaskInfo队列
     */
    private static Map<String, Queue<SourceTaskInfo>> proSourceTask = new ConcurrentHashMap<>();
    /**
     * jdbcTemplate链接器
     */
    private JdbcTemplate jdbcTemplate;

    public OracleSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        proSourceTask.put(getProcNameAndBatchNo(), taskMetadataQueue);
        jdbcTemplate = OracleConnection.getJdbcTemplate(getProcNameAndBatchNoAndSourceDsName());
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
        String sql = "select * from USER_TABLES";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> dbTableNameMapTemp : dbTableMapList) {
            //这里为schema不为库名
            String dbSchemaName = dbTableNameMapTemp.get("TABLESPACE_NAME").toString();
            String tableName = dbTableNameMapTemp.get("TABLE_NAME").toString();
            String dbTable = dbSchemaName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTableNameSet.add(dbTable);
                dbTableNameMap.put(dbTable, dbTable);
            }
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
        OracleSourceSplitRange sourceSplitRange = new OracleSourceSplitRange(sourceDsName, proName, batchNo);
        List<Range> rangeList = sourceSplitRange.getRangeList(dbTableName);
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
        Runnable runnable = () -> {
            while (true) {
                try {
                    if (SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0) > 10) {
                        TimeUnit.SECONDS.sleep(10);
                    }
                    SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                    if (taskMetadata != null) {
                        SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new OracleSourceTask(taskMetadata, programInfo));
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
        };
        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);
    }

    @Override
    public void executeQueryTask() {
        SourceTaskInfo taskMetadata = new SourceTaskInfo();
        taskMetadata.setSourceDsName(sourceDsName);
        Range range = new Range();
        taskMetadata.setDbTableName(programInfo.getDbTableName());
        range.setDbTableName(programInfo.getDbTableName());
        range.setQuery(programInfo.getQuerySql());
        taskMetadata.setRange(range);
        SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new OracleSourceTask(taskMetadata, programInfo));
    }

    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo taskMetadata) {
        proSourceTask.get(procNameAndBatchNo).add(taskMetadata);
    }
}
