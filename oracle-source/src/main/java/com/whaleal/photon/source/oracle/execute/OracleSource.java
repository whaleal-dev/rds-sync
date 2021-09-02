package com.whaleal.photon.source.oracle.execute;

import cache.MemoryCache;
import com.whaleal.photon.source.oracle.split.OracleSourceSplitRange;
import com.whaleal.photon.source.oracle.task.OracleSourceTask;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import conf.Configuration;
import datasource.DBUtil;
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

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();
    //数据库的连接对象
    Connection connection = null;
    JdbcTemplate jdbcTemplate = null;

    public OracleSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        connection = OracleConnection.getConnection(sourceName);
        jdbcTemplate = OracleConnection.getJdbcTemplate(sourceName);
    }

    @Override
    public void createTask() {
        // 遍历执行源数据源抽取
        // 获取数据源的全部库表
        getAllDbCollections(sourceName);
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);

    }

    @Override
    public void getAllDbCollections(String sourceName) {
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList("select * from USER_TABLES");
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("TABLESPACE_NAME").toString();
            String tableName = dbTableNameMap.get("TABLE_NAME").toString();
            String dbTable = dbSchemaName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.put(dbTable, tableName);
            }
        }
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public void startFromSource(String sourceName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceName, next.getValue());
            dbTables.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceName, String dbTableName) {
        OracleSourceSplitRange source = new OracleSourceSplitRange(sourceName);
        List<Range> rangeList = source.getRangeList(dbTableName);
        for (Range range : rangeList) {
            SourceTaskInfo sourceTaskInfo = new SourceTaskInfo();
            sourceTaskInfo.setSourceDsName(sourceName);
            sourceTaskInfo.setDbTableName(dbTableName);
            sourceTaskInfo.setRange(range);
            procSourceTask.get(proName).add(sourceTaskInfo);
        }
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = () -> {
            while (true) {
                try {
                    if (SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0) > 10) {
                        TimeUnit.SECONDS.sleep(10);
                    }
                    SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                    if (taskMetadata != null) {
                        SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
                        SourceTaskPoolManager.submit(proName, new OracleSourceTask(taskMetadata, proName, memoryCache, 128));
                    } else {
                        if (taskMetadataQueue.size() == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(proName, 0) == 0) {
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
        SysPoolManager.submit(proName, runnable);
    }
}
