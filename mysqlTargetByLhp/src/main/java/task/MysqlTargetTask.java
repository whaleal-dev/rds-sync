package task;

import cache.MemoryCache;
import common.column.AbstractColumn;
import common.columnclass.ColumnType;
import common.dataclass.BatchDataEntity;
import common.dbtype.DbTypeFlag;
import common.taskbase.AbstractTargetTask;
import conf.Configuration;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import parse.ParseColumnDataToMysql;
import parse.ParseTypeFromColumn;
import util.Log;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */

public class MysqlTargetTask extends AbstractTargetTask {

    private static Map<String, ColumnType> columnTypeMap = new ConcurrentHashMap<>();
    private static volatile Set<String> dbTableSet = new CopyOnWriteArraySet<>();

    /**
     * jdbcTemplate
     */
    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源链接tcp
     */
    private Connection connection;

    private List<String> writeModels = new ArrayList<>();

    public MysqlTargetTask(Configuration configuration, MemoryCache memoryCache) {
        super(configuration, memoryCache);
        this.connection = MySqlConnection.getConnection(this.targetDsName);
    }

    @Override
    public void run() {
        applyData();
    }

    static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * applyData 应用数据
     *
     * @desc 应用数据
     */
    @Override
    public void applyData() {
        Log.info("启动targetMysql任务:" + this.targetDsName);
        while (true) {
            BatchDataEntity batchDataEntity = memoryCache.getData();
            try {
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的mongoNamespace
                    if (!dbTableSet.contains(batchDataEntity.getDbTableName().toUpperCase())) {
                        createTableByCommonDataEntity(batchDataEntity.getDbTableName(), batchDataEntity.getDataList().get(0), targetDsName);
                    }
                    System.out.println("targetMysql:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
                    // 判断操作行为。如果为INSERTMANY类型，直接应用数据。
                    parseColumnDataToDocument(batchDataEntity);
                    bulkExecute(this.writeModels, "", -1);
                    this.writeModels = new ArrayList<>();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void parseColumnDataToDocument(BatchDataEntity batchDataEntity) {

        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            String insertSql = "insert into " + batchDataEntity.getDbTableName();
            String columns = "(";
            String values = "values(";
            for (AbstractColumn columnData : columnList) {
                Object value = ParseColumnDataToMysql.parseColumnData(columnData);
                columns += "`" + columnData.getColumnName() + "`,";
                values += value + " , ";
            }
            columns = columns.substring(0, columns.length() - 1);
            values = values.substring(0, values.length() - 2);
            columns += ")";
            values += ") ";
            insertSql = insertSql + columns + values;
            writeModels.add(insertSql);
        }
    }

    @Override
    public void bulkExecute(String dbTable, long batchNo) {

    }

    /**
     * createConnection 创建mysql的tcp链接
     *
     * @desc 创建mysql的tcp链接
     */
    private void createConnection() {
        try {
            this.connection = this.jdbcTemplate.getDataSource().getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
    }


    /**
     * bulkExecute 批量写数据
     *
     * @param sqlList 数据集合
     * @desc 批量写数据
     */
    public void bulkExecute(List<String> sqlList, String dbTable, long batchNo) {

        try {
            Statement statement = connection.createStatement();
            connection.setAutoCommit(false);
            for (String sql : sqlList) {
                if (sql == null || sql.length() < 2 || sql.equalsIgnoreCase("show tables")) {
                    continue;
                }
                statement.addBatch(sql);
            }
            Long start = System.currentTimeMillis();
            statement.executeBatch();
            Long end = System.currentTimeMillis();
            connection.commit();
            statement.clearBatch();
            Log.info("dbTable:" + dbTable + ",耗时处理:" + (end - start) + ",sqlListSize:" + sqlList.size());
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(sqlList.get(0));
            try {
                connection.rollback();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }

        }
    }

    /**
     * getTableInfoByTableName 获取表结构
     *
     * @param dbTable
     * @desc 获取表结构
     */
    public static synchronized int getTableInfoByTableName(String dbTable, String targetDsName) {
        String[] array = dbTable.split("\\.", 2);
        String dbName = array[0];
        String tableName = array[1];
        String sql = "select *  from information_schema.COLUMNS t where t.TABLE_SCHEMA ='" + dbName + "' and t.TABLE_NAME  ='" + tableName + "' ";
        List<Map<String, Object>> mysqlColumnMap = MySqlConnection.getJdbcTemplate(targetDsName).queryForList(sql);
        if (mysqlColumnMap.size() == 0 || dbTableSet.contains(dbTable.toUpperCase())) {
            return 0;
        }
        for (Map<String, Object> tableInfo : mysqlColumnMap) {
            ColumnType columnAndType = new ColumnType();
            String columnName = tableInfo.get("COLUMN_NAME").toString();
            columnAndType.setColumnName(columnName);
            String dataType = tableInfo.get("DATA_TYPE").toString();
            columnAndType.setColumnType(dataType);
            if (tableInfo.get("COLUMN_TYPE") != null) {
                String columnType = tableInfo.get("COLUMN_TYPE").toString();
                String[] columnTypeArray = columnType.split("\\(|,|\\)");
                columnAndType.setColumnType(columnTypeArray[0]);
                if (columnTypeArray.length == 3) {
                    columnAndType.setPrecision(Integer.parseInt(columnTypeArray[2]));
                    columnAndType.setLength(Integer.parseInt(columnTypeArray[1]));
                }
                if (columnTypeArray.length == 2) {
                    columnAndType.setLength(Integer.parseInt(columnTypeArray[1]));
                }
            }
            columnTypeMap.put((dbTable + ":" + columnName).toUpperCase(), columnAndType);
        }
        dbTableSet.add((dbTable).toUpperCase());
        return mysqlColumnMap.size();
    }


    public synchronized static void createTableByCommonDataEntity(String dbTable, List<AbstractColumn> columnDataList, String targetDsName) {
        if (dbTableSet.contains(dbTable.toUpperCase())) {
            return;
        }
        String[] array = dbTable.split("\\.", 2);
        String dbName = array[0];
        String tableName = array[1];
        String sql = "select count(*)  from information_schema.TABLES t where t.TABLE_SCHEMA ='" + dbName + "' and t.TABLE_NAME  ='" + tableName + "' ";
        // 查询源数据源中是否已有该表
        int count = MySqlConnection.getJdbcTemplate(targetDsName).queryForObject(sql, Integer.class);
        if (count == 1) {
            getTableInfoByTableName(dbTable, targetDsName);
            return;
        }
        MySqlConnection.getJdbcTemplate(targetDsName).execute("CREATE DATABASE IF NOT EXISTS " + dbName);
        StringBuilder createSql = new StringBuilder("create table if not exists " + dbName + "." + tableName + " ( ");
        for (AbstractColumn columnValue : columnDataList) {
            if (columnValue.getData() != null) {
                ColumnType columnType = ParseTypeFromColumn.parseType(columnValue);
                columnType.setDescType(DbTypeFlag.MYSQL);
                createSql.append(columnType.toString() + ",");
            }
        }
        createSql.deleteCharAt(createSql.length() - 1);
        createSql.append(" ) ");
        MySqlConnection.getJdbcTemplate(targetDsName).execute(createSql.toString());
        getTableInfoByTableName(dbTable, targetDsName);
        Log.info("dbTableName:" + dbTable + ",createSql: " + createSql);
    }

}
