package main;


import cache.MemoryCache;
import com.alibaba.fastjson.JSONObject;
import common.taskbase.metadata.SourceMetadata1;
import common.taskbase.metadata.SourceTaskInfo1;
import conf.Configuration;
import constant.Key;
import org.apache.commons.lang3.StringUtils;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.DBUtil;
import util.Log;
import util.ReaderSplitUtil;

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
public class MysqlSource extends SourceMetadata1 {

    public MysqlSource(Configuration configuration, MemoryCache memoryCache) {
        File file = new File("/Users/jiangyun/Documents/3.json");
        this.configuration = Configuration.from(file);
        this.taskName = configuration.getString("taskName", "");
        this.proName = configuration.getString("proName", "");
        this.memoryCache = memoryCache;

        procSourceTask.put(proName, taskMetadataQueue1);
    }

    @Override
    public void createTask() throws SQLException {
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 遍历执行源数据源抽取
//        File file = new File("/Users/jiangyun/Documents/3.json");
        // 获取数据源的全部库表
        getAllDbTables(configuration);
        // 开始遍历抽取该数据源的所有库表
        startFromSource(configuration, false);
        isOver = true;
    }

    @Override
    public void startFromSource(Configuration conf, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(conf, next.getValue());
        }
    }

    @Override
    public void createSourceEntity(Configuration conf, String dbTableName) {
        //根据总配置进行切分配置
        List<Configuration> list = ReaderSplitUtil.doSplit(conf, conf.getInt("adviceNumber", 2));
        for (Configuration splitConf : list) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    SourceTaskInfo1 taskMetadata = SourceTaskInfo1.builder().rangeSql(splitConf.getString(Key.QUERY_SQL))
                            .sourceUrl(splitConf.getString(Key.JDBC_URL)).databaseType(splitConf.getString(Key.DATABASE_TYPE))
                            .sourceUsername(splitConf.getString(Key.USERNAME)).sourcePassword(splitConf.getString(Key.PASSWORD))
                            .sourceDatabase(splitConf.getString(Key.DATABASE)).sourceTable(splitConf.getString(Key.TABLE))
                            .targetUrl(splitConf.getString("target.jdbcUrl")).targetUsername(splitConf.getString("target.username", null))
                            .targetPassword(splitConf.getString("target.password", null)).targetDatabase(splitConf.getString(Key.DATABASE))
                            .targetCollection(StringUtils.strip(splitConf.getString("target.collection"), "[]").replaceAll("\"", "")).dataBatchSize(splitConf.getInt("dataBatchSize"))
                            .build();
                    Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                    pushTaskMeta(proName, taskMetadata);
                }
            };
            SysPoolManager.submit(proName, runnable);
        }
    }
    
    @Override
    public void getAllDbTables(Configuration conf) throws SQLException {
        List<JSONObject> connConfList = conf.getList(Key.CONNECTION, JSONObject.class);
        Connection conn = DBUtil.getConnection(conf);
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = metaData.getTables(null, null, "%", types);
        Map<String, String> dbTables = new HashMap<>();
        while(rs.next()){
            //1 TABLE_CAT String => table catalog (may be null)
            //2 TABLE_SCHEM String => table schema (may be null)
            //3 TABLE_NAME String => table name
            String table = rs.getString(3);
            //读取配置中的table
            String tableConf = StringUtils.strip(connConfList.get(0).getString(Key.TABLE), "[]")
                    .replaceAll("\"", "");
            if (table.equals(tableConf)) {
                dbTables.put(table, tableConf);
            }
        }
        rs.close();
        Log.info("sourceName:  " + conf.getString("database") + ",全量同步的表列表:  " + dbTables);
    }

    @Override
    public  void  submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (taskMetadataQueue1.size() == 0) {
                            TimeUnit.SECONDS.sleep(2);
                        }
                        SourceTaskInfo1 taskMetadata = taskMetadataQueue1.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata, proName, memoryCache, 128));
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                    }
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    protected static Map<String, Queue<SourceTaskInfo1>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procName, SourceTaskInfo1 taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }

}
