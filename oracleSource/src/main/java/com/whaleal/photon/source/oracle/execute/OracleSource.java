package com.whaleal.photon.source.oracle.execute;

import cache.MemoryCache;
import com.whaleal.photon.source.oracle.split.OracleSourceSplitRange;
import com.whaleal.photon.source.oracle.task.OracleSourceTask;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import common.photonV.entity.ProgramInfo;
import dbconnection.oracle.OracleConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * orcle数据执行器
 *
 * @author cs
 * @date 2021/08/30
 */
@Slf4j
public class OracleSource extends SourceMetadata {

    private static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    private JdbcTemplate jdbcTemplate;

    public OracleSource(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        procSourceTask.put(procNameAndBatchNo, taskMetadataQueue);
        jdbcTemplate = OracleConnection.getJdbcTemplate(procNameAndBatchNoAndSourceDsName);
    }

    @Override
    public void createTask() {
        // 获取数据源的全部库表
        getAllDbCollections(sourceDsName);
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceDsName, false);
    }

    @Override
    public void getAllDbCollections(String sourceDsName) {
        String sql = "select * from USER_TABLES";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("TABLESPACE_NAME").toString();
            String tableName = dbTableNameMap.get("TABLE_NAME").toString();
            String dbTable = dbSchemaName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.put(dbTable, dbTable);
            }
        }
        Log.info("sourceName:" + sourceDsName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public void startFromSource(String sourceDsName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceDsName, next.getValue());
            dbTables.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceDsName, String dbTableName) {
        OracleSourceSplitRange sourceSplitRange = new OracleSourceSplitRange(sourceDsName,proName,batchNo);
        List<Range> rangeList = sourceSplitRange.getRangeList(dbTableName);
        for (Range range : rangeList) {
            SourceTaskInfo sourceTaskInfo = new SourceTaskInfo();
            sourceTaskInfo.setSourceDsName(sourceDsName);
            sourceTaskInfo.setDbTableName(dbTableName);
            sourceTaskInfo.setRange(range);
            procSourceTask.get(procNameAndBatchNo).add(sourceTaskInfo);
        }
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = () -> {
            while (true) {
                try {
                    if (SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0) > 10) {
                        TimeUnit.SECONDS.sleep(10);
                    }
                    SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                    if (taskMetadata != null) {

                        SourceTaskPoolManager.submit(procNameAndBatchNo, new OracleSourceTask(taskMetadata, proName, memoryCache, 128,batchNo));
                    } else {
                        boolean isOver = taskMetadataQueue.size() == 0 && SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0) == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0) == 0;
                        if (isOver) {
                            break;
                        }
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (InterruptedException e) {
                    Log.error(e.getMessage());
                    break;
                }
            }
        };
        SysPoolManager.submit(procNameAndBatchNo, runnable);
    }
}
