package execute;

import cache.MemoryCache;
import common.dataclass.Range;
import common.taskbase.metadata.MysqlSourceMetadata;
import common.taskbase.MysqlSourceTaskInfo;
import conf.Configuration;
import conf.ReaderSplitUtil;
import dbconnection.mysql.MySqlConnection;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;
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
public class MysqlSource extends MysqlSourceMetadata {

    Connection connection = null;

    public MysqlSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, mysqlTaskMetadataQueue);
        connection = MySqlConnection.getConnection(sourceName);
    }

    @Override
    public void createTask(){
        // 遍历执行源数据源抽取
        // 获取数据源的全部库表
        try {
            getAllDbTables(sourceName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
    }

    @Override
    public void startFromSource(String sourceName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            //TODO
            createSourceEntity(configuration, next.getValue());
            dbTables.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(Configuration configuration, String dbTableName) {
        int adviceNumber = configuration.getAdviceNumber();
        //根据总配置进行切分配置
        List<Range> list = new ArrayList<>();
        try {
            list = ReaderSplitUtil.doSplit(configuration, adviceNumber,
                    ReaderSplitUtil.getTableNumber(configuration.getSourceDsName(), configuration.getDbTableWhite()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        List<Range> finalList = list;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (Range splitRange : finalList) {
                    SysPoolManager.setSysActiveThreadNum(proName, 1);
                    //TODO
                    MysqlSourceTaskInfo taskMetadata = new MysqlSourceTaskInfo(splitRange.getQuery(),
                            splitRange.getDbTableName(), sourceName);
                    // Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                    pushTaskMeta(proName, taskMetadata);
                    SysPoolManager.setSysActiveThreadNum(proName, -1);
                }
//                    MysqlSourceTaskInfo taskMetadata = MysqlSourceTaskInfo.builder().rangeSql(splitConf.getString(Key.QUERY_SQL))
//                            .sourceUrl(splitConf.getString(Key.JDBC_URL)).databaseType(splitConf.getString(Key.DATABASE_TYPE))
//                            .sourceUsername(splitConf.getString(Key.USERNAME)).sourcePassword(splitConf.getString(Key.PASSWORD))
//                            .sourceDatabase(splitConf.getString(Key.DATABASE)).sourceTable(splitConf.getString(Key.TABLE))
//                            .targetUrl(splitConf.getString("target.jdbcUrl")).targetUsername(splitConf.getString("target.username", null))
//                            .targetPassword(splitConf.getString("target.password", null)).targetDatabase(splitConf.getString(Key.DATABASE))
//                            .targetCollection(StringUtils.strip(splitConf.getString("target.collection"), "[]").replaceAll("\"", "")).dataBatchSize(splitConf.getInt("dataBatchSize"))
//                            .build();
//                    Log.info("taskMetadata配置信息:" + taskMetadata.toString());
//                    pushTaskMeta(proName, taskMetadata);
            }
        };
        SysPoolManager.submit(proName, runnable);

    }
    
    @Override
    public void getAllDbTables(String sourceName) throws SQLException {
//        List<JSONObject> connConfList = conf.getList(Key.CONNECTION, JSONObject.class);
//        Connection conn = DBUtil.getConnection(conf);
        //获取连接
        Connection conn = MySqlConnection.getConnection(sourceName);
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = metaData.getTables(null, null, "%", types);
        Map<String, String> dbTables = new HashMap<>();
        while(rs.next()){
            //1 TABLE_CAT String => table catalog (may be null)
            //2 TABLE_SCHEM String => table schema (may be null)
            //3 TABLE_NAME String => table name
            //获取数据库表名
            String tableName = rs.getString(3);
            //获取数据库名
            String dbName = rs.getString(1);
            //读取配置中的table
//            String tableConf = StringUtils.strip(connConfList.get(0).getString(Key.TABLE), "[]").replaceAll("\"", "");
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.put(dbTable, dbTable);
            }
        }
        rs.close();
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public  void  submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        MysqlSourceTaskInfo taskMetadata = mysqlTaskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata, proName, memoryCache, 128));
                        } else {
                            if (mysqlTaskMetadataQueue.size() == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(proName, 0) == 0) {
                                break;
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }

                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                    }
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    protected static Map<String, Queue<MysqlSourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procName, MysqlSourceTaskInfo taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }

}
