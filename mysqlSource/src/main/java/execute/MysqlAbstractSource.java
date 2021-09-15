package execute;

import cache.MemoryCache;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.AbstractSourceExecute;
import common.photonV.entity.ProgramInfo;
import dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import sourcesplit.MysqlSourceSplitRange;
import task.MysqlSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlAbstractSource extends AbstractSourceExecute {

    private Connection connection;
    private JdbcTemplate jdbcTemplate;


    public MysqlAbstractSource(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.programInfo = programInfo;
        procSourceTask.put(procNameAndBatchNo, taskMetadataQueue);
        connection = MySqlConnection.getConnection(procNameAndBatchNoAndSourceDsName);
        jdbcTemplate = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndSourceDsName);
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
    public void startFromSource(String sourceDsName, boolean isParallel) {
        createSourceEntity(sourceDsName, "");
        dbTables = new ConcurrentHashMap<>();
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceDsName, String dbTableName) {
        List<Range> list = new ArrayList<>();
        try {
            list = MysqlSourceSplitRange.doSplit(programInfo);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
        for (Range splitRange : list) {
            SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 1);
            SourceTaskInfo taskMetadata = new SourceTaskInfo(splitRange, splitRange.getDbTableName(), sourceDsName);
            Log.error("taskMetadata" + taskMetadata);
            pushTaskMeta(procNameAndBatchNo, taskMetadata);
            SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, -1);
        }
        Log.info("  切分数     =   " + list.size());

    }

    @Override
    public void getAllDbCollections(String sourceDsName) {
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from information_schema.TABLES");
        for (Map dbTableMap : dbTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            //忽略 mysql 系统表
            if (dbName.equalsIgnoreCase("mysql") ||
                    dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTableNameSet.add(dbTable);
                dbTables.put(dbTable, dbTable);
            }
        }
        Log.info("sourceDsName:" + sourceDsName + ",全量同步的表列表:" + dbTables);
    }

    public Map<String, String> getTableStructure() {
        Map<String, String> tableStructureMap = new HashMap<>();
        for (String dbTableName : dbTableNameSet) {
            try {
                Map<String, Object> map = jdbcTemplate.queryForMap("show create table " + dbTableName);
                String tableName = dbTableName.split("\\.", 2)[1];
                String createTableSql = map.get("Create Table").toString().split("\\) ENGINE=")[0] + ")";
                createTableSql = createTableSql.replaceFirst("CREATE TABLE `" + tableName + "`", "CREATE TABLE " + dbTableName + "");
                createTableSql=createTableSql.replaceAll("CHARACTER SET \\w+ COLLATE \\w+", " ");
                tableStructureMap.put(dbTableName, createTableSql);
            } catch (Exception e) {

            }

        }
        return tableStructureMap;
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

                            SourceTaskPoolManager.submit(procNameAndBatchNo, new MysqlSourceTask(taskMetadata, proName, memoryCache, 128, batchNo));
                        } else {
                            boolean isOver = taskMetadataQueue.size() == 0 && SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0) == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0) == 0;
                            if (isOver) {
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
        SysPoolManager.submit(procNameAndBatchNo, runnable);
    }

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procNameAndBatchNo).add(taskMetadata);
    }

}
