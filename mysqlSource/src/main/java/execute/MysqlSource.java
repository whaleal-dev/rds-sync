package execute;

import cache.MemoryCache;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import conf.Configuration;
import sourcesplit.ReaderSplitUtil;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;

import java.sql.Connection;
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
    JdbcTemplate jdbcTemplate = null;

    public MysqlSource(Configuration configuration, MemoryCache memoryCache) {
        this.configuration = configuration;
        this.sourceName = configuration.getSourceDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        connection = MySqlConnection.createConnection(sourceName, DataSourceUtil.getDataSourceByDsName(sourceName));
        jdbcTemplate = MySqlConnection.getJdbcTemplate(sourceName);
    }

    @Override
    public void createTask() {
        // 遍历执行源数据源抽取
        // 获取数据源的全部库表
        try {
            getAllDbCollections(sourceName);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
        System.out.println("1========");
        // 启动获取提交Task任务的线程
        submitSourceTask();
        System.out.println("2========");
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
        System.out.println("3========");
    }


    @Override
    public void startFromSource(String sourceName, boolean isParallel) {
        System.out.println("0========");
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            System.out.println("9======");
            createSourceEntity(sourceName, next.getValue());
            dbTables.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceName, String dbTableName) {
        System.out.println("7======");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("8======");
                //根据总配置进行切分配置
                List<Range> list = new ArrayList<>();
                try {
                    System.out.println("====" + configuration);
                    list = ReaderSplitUtil.doSplit(configuration, configuration.getAdviceNumber(),
                            ReaderSplitUtil.getTableNumber(configuration));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.error(e.getMessage());
                }
                System.out.println("10======");
                for (Range splitRange : list) {
                    SysPoolManager.setSysActiveThreadNum(proName, 1);
                    //TODO
                    SourceTaskInfo taskMetadata = new SourceTaskInfo(splitRange,
                            dbTableName, sourceName);
                    System.out.println("6======");
                    // Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                    pushTaskMeta(proName, taskMetadata);
                    SysPoolManager.setSysActiveThreadNum(proName, -1);
                }
            }
        };
        SysPoolManager.submit(proName, runnable);

    }

    @Override
    public void getAllDbCollections(String sourceName) throws SQLException {
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from information_schema.TABLES");
        for (Map dbTableMap : dbTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            if (dbName.equalsIgnoreCase("mysql") || dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.put(dbTable, dbTable);
            }
        }
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
        System.out.println(taskMetadataQueue);
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        System.out.println("4========");
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            Log.info("MysqlSourceTaskInfo");
                            SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
                            SourceTaskPoolManager.submit(proName, new MysqlSourceTask(taskMetadata, proName, memoryCache, 128));
                        } else {
                            if (taskMetadataQueue.size() == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(proName, 0) == 0) {
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

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procName, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }

}
