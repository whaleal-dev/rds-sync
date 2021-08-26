package main;


import cache.MemoryCache;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata1;
import common.taskbase.metadata.SourceTaskInfo1;
import conf.Configuration;
import constant.Key;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.DBUtil;
import util.DataBaseType;
import util.Log;
import util.ReaderSplitUtil;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
        this.taskName = configuration.getString("taskName", null);
        this.proName = configuration.getString("proName", null);
        this.memoryCache = memoryCache;

        procSourceTask.put(proName, taskMetadataQueue1);
    }

    @Override
    public void createTask() throws SQLException {
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 遍历执行源数据源抽取
        File file = new File("/Users/jiangyun/Documents/3.json");
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

        List<Configuration> list = ReaderSplitUtil.doSplit(conf, conf.getInt("adviceNumber", 2));

        for (Configuration splitConf : list) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    String rangeSql = splitConf.getString(Key.QUERY_SQL);
                    String sourceUrl = splitConf.getString(Key.JDBC_URL);
                    String sourceUsername = splitConf.getString(Key.USERNAME);
                    String sourcePassword = splitConf.getString(Key.PASSWORD);
                    String sourceDatabase = splitConf.getString("database");
                    String soureTable = splitConf.getString(Key.TABLE);
                    String targetUrl = splitConf.getString("target.jdbcUrl");
                    String targetUsername = splitConf.getString("target.username", null);
                    String targetPassword = splitConf.getString("target.password", null);
                    String sqtargetDatabase = splitConf.getString("target.database");
                    String targetCollection = splitConf.getString("target.collection");
                    Integer dataBatchSize = splitConf.getInt("dataBatchSize");

                    SourceTaskInfo1 taskMetadata = new SourceTaskInfo1(rangeSql,sourceUrl,sourceUsername,sourcePassword,
                            sourceDatabase, soureTable, targetUrl, targetUsername, targetPassword, sqtargetDatabase,
                            targetCollection, dataBatchSize);
                    Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                    pushTaskMeta(proName, taskMetadata);

                }
            };
            SysPoolManager.submit(proName, runnable);
        }
    }
    
    @Override
    public void getAllDbTables(Configuration conf) throws SQLException {
        Connection conn = DBUtil.getConnection(DataBaseType.MySql, conf.getString("connection.jdbcUrl"),
                conf.getString(Key.USERNAME), conf.getString(Key.PASSWORD));
        //检索元数据对象
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        //检索数据库中的列
        ResultSet tables = metaData.getTables(null, null, "%", types);
        while (tables.next()) {
            System.out.println(tables.getString("TABLE_NAME"));
            String dbTable = tables.toString();
            //TODO 没指定表名
            if (dbTable.equals(conf.getString(Key.TABLE))) {
                dbTables.put(dbTable, dbTable);
            }
            //检测表明是否匹配给定的正则表达式。
//            if (dbTable.matches(conf.getString(Key.TABLE))) {
//                dbTables.put(dbTable, dbTable);
//            }
        }
/*        MongoIterable<String> mongoIterableOfDb = mongoClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            System.out.println(dbTables);
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") || dbName.equalsIgnoreCase("config")) {
                Log.info("admin,local,config库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = MongoDbConnection.getMongoClient(sourceName).getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                if ((dbTable).matches(dbTableWhite)) {
                    dbTables.put(dbTable, dbTable);
                }
            }
        }*/
        Log.info("sourceName:" + conf.getString("database") + ",全量同步的表列表:" + dbTables);
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
                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata));
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
