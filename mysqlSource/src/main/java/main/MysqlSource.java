package main;


import cache.MemoryCache;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import common.taskbase.metadata.SourceTaskInfo1;
import conf.Configuration;
import conf.DBUtil;
import conf.ReaderSplitUtil;
import constant.Key;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlSource extends SourceMetadata {
    Connection connection = null;

    public MysqlSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        connection = MySqlConnection.getConnection(sourceName);
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
    public void getAllDbCollections(String sourceName) throws SQLException {
        Connection conn = MySqlConnection.getConnection(sourceName);
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = metaData.getTables(null, null, "%", types);
        Map<String, String> dbTables = new HashMap<>();
        while (rs.next()) {
            //1 TABLE_CAT String => table catalog (may be null)
            //2 TABLE_SCHEM String => table schema (may be null)
            //3 TABLE_NAME String => table name
            String table = rs.getString(3);
            //读取配置中的table
                dbTables.put(table, table);
        }
        rs.close();
        Log.info("sourceName:  ,全量同步的表列表:  " + dbTables);
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
        MongodbSourceSplitRange source = new MongodbSourceSplitRange(sourceName);
        Map<Integer, Range> map = source.getIdTypes(dbTableName);
        Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (rangeMap.hasNext()) {
                    SysPoolManager.setSysActiveThreadNum(proName, 1);
                    //System.out.println("getSysThreadNum1:" + SysPoolManager.setSysActiveThreadNum(proName, 0));
                    Map.Entry<Integer, Range> next = rangeMap.next();
                    Range rangeOfTable = next.getValue();
                    while (rangeOfTable.getMinId() != null) {
                        Range range = source.splitRange(dbTableName, rangeOfTable, next.getKey());
                        SourceTaskInfo taskMetadata = new SourceTaskInfo(range, dbTableName, sourceName);
                        // Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                        pushTaskMeta(proName, taskMetadata);
                    }
                    SysPoolManager.setSysActiveThreadNum(proName, -1);
                    //System.out.println("getSysThreadNum2:" + SysPoolManager.setSysActiveThreadNum(proName, 0));
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
                            SourceTaskPoolManager.submit(proName, new MongodbSourceTask(taskMetadata, proName, memoryCache, 128));
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
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procName, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }

}
