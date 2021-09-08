package task;

import cache.MemoryCache;
import common.column.AbstractColumn;
import common.column.StringColumn;
import common.columnclass.ColumnType;
import common.dataclass.BatchDataEntity;
import common.dbtype.DbTypeFlag;
import common.dbtype.MySqlType;
import common.taskbase.AbstractTargetTask;
import common.photonV.entity.ProgramInfo;
import dbconnection.mysql.MySqlConnection;
import parse.ColumnDataToMysqlData;
import parse.ParseTypeFromColumnType;
import thread.TargetTaskPoolManager;
import util.Log;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */

public class MysqlTargetTask extends AbstractTargetTask {

    /**
     * 字段Map
     * k=dsName+dbTableName+columnName
     */
    private static volatile Map<String, ColumnType> columnTypeMap = new ConcurrentHashMap<>();
    /**
     * 库表名单
     * dsName+dbTableName
     */
    private static volatile Set<String> dbTableSet = new CopyOnWriteArraySet<>();


    /**
     * 数据源链接tcp
     */
    private Connection connection;
    /**
     * 拼接后的sql
     */
    private List<String> sqlList = new ArrayList<>();


    public MysqlTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.connection = MySqlConnection.getConnection(procNameAndBatchNoAndTargetDsName);

    }

    @Override
    public void run() {
        Log.info("启动target任务:" + this.targetDsName);
        try {
            applyData();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, -1);
        }
    }


    /**
     * applyData 应用数据
     *
     * @desc 应用数据
     */
    @Override
    public void applyData() {
        while (true) {
            try {
                if (AbstractTargetTask.getIsStopFlagOfTarget(procNameAndBatchNo)) {
                    break;
                }
                BatchDataEntity batchDataEntity = memoryCache.getData();
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的mongoNamespace
                    this.dbTableName = batchDataEntity.getDbTableName();
                    if (!dbTableSet.contains(batchDataEntity.getDbTableName().toUpperCase())) {
                        createTableByCommonDataEntity(batchDataEntity.getDbTableName(), batchDataEntity.getDataList().get(0), targetDsName, procNameAndBatchNoAndTargetDsName);
                    }
                    parseColumnDataToTargetData(batchDataEntity);
                    bulkExecute(dbTableName, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void parseColumnDataToTargetData(BatchDataEntity batchDataEntity) {

        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();

        for (List<AbstractColumn> columnList : dataList) {
            checkDataIsCorrect(columnList, targetDsName, dbTableName, procNameAndBatchNoAndTargetDsName);
            String insertSql = "insert into " + batchDataEntity.getDbTableName();
            String columns = "(";
            String values = "values(";
            for (AbstractColumn columnData : columnList) {
                //  System.out.println("columnName:         " + columnData.getColumnName() + "           data:   " + columnData.getData());
                Object value = ColumnDataToMysqlData.parseColumnData(columnData);
                if (value == null) {
                    continue;
                }
                columns += "`" + columnData.getColumnName() + "`,";
                values += value + " , ";
            }
            columns = columns.substring(0, columns.length() - 1);
            values = values.substring(0, values.length() - 2);
            columns += ")";
            values += ") ";
            insertSql = insertSql + columns + values;
            sqlList.add(insertSql);
        }
    }


    @Override
    public void bulkExecute(String dbTable, long batchNo) {
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
        } finally {
            this.sqlList = new ArrayList<>();
        }
    }


    /**
     * getTableInfoByTableName 获取表结构
     *
     * @param dbTable
     * @desc 获取表结构
     */
    public static synchronized int getTableInfoByTableName(String dbTable, String targetDsName, String procNameAndBatchNoAndTargetDsName) {
        String[] array = dbTable.split("\\.", 2);
        String dbName = array[0];
        String tableName = array[1];
        String sql = "select *  from information_schema.COLUMNS t where t.TABLE_SCHEMA ='" + dbName + "' and t.TABLE_NAME  ='" + tableName + "' ";
        List<Map<String, Object>> mysqlColumnMap = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).queryForList(sql);
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
            columnTypeMap.put((targetDsName + ":" + dbTable + ":" + columnName).toUpperCase(), columnAndType);
        }
        dbTableSet.add((dbTable).toUpperCase());
        return mysqlColumnMap.size();
    }


    public synchronized static void createTableByCommonDataEntity(String dbTable, List<AbstractColumn> columnDataList, String targetDsName, String procNameAndBatchNoAndTargetDsName) {
        if (dbTableSet.contains(dbTable.toUpperCase())) {
            return;
        }
        String[] array = dbTable.split("\\.", 2);
        String dbName = array[0];
        String tableName = array[1];
        String sql = "select count(*)  from information_schema.TABLES t where t.TABLE_SCHEMA ='" + dbName + "' and t.TABLE_NAME  ='" + tableName + "' ";
        // 查询源数据源中是否已有该表
        int count = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).queryForObject(sql, Integer.class);
        if (count == 1) {
            getTableInfoByTableName(dbTable, targetDsName, procNameAndBatchNoAndTargetDsName);
            return;
        }
        MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).execute("CREATE DATABASE IF NOT EXISTS " + dbName);
        StringBuilder createSql = new StringBuilder("create table if not exists " + dbName + "." + tableName + " ( ");
        for (AbstractColumn columnValue : columnDataList) {
            if (columnValue.getData() != null) {
                ColumnType columnType = ParseTypeFromColumnType.parseType(columnValue);
                columnType.setDescType(DbTypeFlag.MYSQL);
                createSql.append(columnType.toString() + ",");
            }
        }
        createSql.deleteCharAt(createSql.length() - 1);
        createSql.append(" ) ");
        MySqlConnection.getJdbcTemplate(targetDsName).execute(createSql.toString());
        getTableInfoByTableName(dbTable, targetDsName, procNameAndBatchNoAndTargetDsName);
        Log.info("dbTableName:" + dbTable + ",createSql: " + createSql);
    }

    /**
     * checkInsertAndUpdateIsCorrect 检查表信息中是否和document字段匹配
     *
     * @param columnDataList
     * @param dsName
     * @desc 检查表信息中是否和document字段匹配
     */
    public static void checkDataIsCorrect(List<AbstractColumn> columnDataList, String dsName, String dbTable, String procNameAndBatchNoAndTargetDsName) {
        try {
            for (AbstractColumn columnValue : columnDataList) {
                if (columnValue.getData() == null) {
                    continue;
                }
                String columnName = columnValue.getColumnName();
                if (columnTypeMap.containsKey((dsName + ":" + dbTable + ":" + columnName).toUpperCase())) {
                    detectionType(dbTable, columnName, columnValue, dsName, procNameAndBatchNoAndTargetDsName);
                    detectionLength(dbTable, columnName, columnValue, dsName, procNameAndBatchNoAndTargetDsName);
                } else {
                    //修改表结构增加字段
                    String addColumnSql = "alter table " + dbTable + " add column ";
                    ColumnType columnType = ParseTypeFromColumnType.parseType(columnValue);
                    addColumnSql += columnType.toString();
                    synchronized (MysqlTargetTask.class) {
                        // dcl
                        if (!columnTypeMap.containsKey((dsName + ":" + dbTable + ":" + columnName).toUpperCase())) {
                            MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).execute(addColumnSql);
                            Log.info("addColumnSql:" + addColumnSql);
                            columnTypeMap.put((dsName + ":" + dbTable + ":" + columnName).toUpperCase(), columnType);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * detectionTypeAndLength 探测类型长度是否正确
     *
     * @param dbTableName
     * @param columnName
     * @param columnValue
     * @param columnName
     * @param dsName
     * @desc 探测类型长度是否正确。此代码需要大幅度优化
     */
    public static void detectionLength(String dbTableName, String columnName, AbstractColumn columnValue, String dsName, String procNameAndBatchNoAndTargetDsName) {
        ColumnType columnType = columnTypeMap.get((dsName + ":" + dbTableName + ":" + columnName).toUpperCase());
        boolean isAlter = ParseTypeFromColumnType.isModifyTypeOrLength(columnValue, columnType);
        if (isAlter) {
            synchronized (MysqlTargetTask.class) {
                columnType = columnTypeMap.get((dsName + ":" + dbTableName + ":" + columnName).toUpperCase());
                if (ParseTypeFromColumnType.isModifyTypeOrLength(columnValue, columnType)) {
                    ColumnType columnTypeTemp = ParseTypeFromColumnType.parseType(columnValue);
                    String alterSql = "alter table " + dbTableName + " modify column" + columnTypeTemp.toString() + " ";
                    MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).execute(alterSql);
                    columnTypeMap.put((dsName + ":" + dbTableName + ":" + columnName).toUpperCase(), columnTypeTemp);
                    Log.info(columnType.getLength() + "." + columnType.getPrecision() + "    =   " + columnValue.getColumnName() + "    =   " + columnValue.getData() + "    =   " + alterSql);
                    Log.info(columnTypeTemp.getLength() + "." + columnTypeTemp.getPrecision() + "    =   " + columnTypeTemp.getColumnName() + "    =   " + columnTypeTemp);

                }
            }
        }
    }

    public static void detectionType(String dbTableName, String columnName, AbstractColumn columnValue, String dsName, String procNameAndBatchNoAndTargetDsName) {
        ColumnType columnType = columnTypeMap.get((dsName + ":" + dbTableName + ":" + columnName).toUpperCase());
        ColumnType columnTypeNew = ParseTypeFromColumnType.parseType(columnValue);
        boolean isAlter = ParseTypeFromColumnType.isModifyType(columnType, columnValue);
        if (columnType.getColumnType().equals(MySqlType.VARCHAR) && !columnTypeNew.getColumnType().contains("CHAR")) {
            columnValue = parseAbstractColumnToStringColumn(columnValue);
        } else if (isAlter) {
            synchronized (MysqlTargetTask.class) {
                ColumnType columnType2 = columnTypeMap.get((dsName + ":" + dbTableName + ":" + columnName).toUpperCase());
                if (ParseTypeFromColumnType.isModifyType(columnType2, columnValue)) {
                    ColumnType columnTypeTemp = new ColumnType();
                    columnTypeTemp.setColumnType(MySqlType.VARCHAR);
                    columnTypeTemp.setDbTableName(dbTableName);
                    columnTypeTemp.setColumnName(columnName);
                    columnTypeTemp.setDescType(DbTypeFlag.MYSQL);
                    columnTypeTemp.setLength((int) (columnValue.getData().toString().length() * 1.5));
                    String alterSql = "alter table " + dbTableName + " modify column" + columnTypeTemp.toString() + " ";
                    MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName).execute(alterSql);
                    columnTypeMap.put((dsName + ":" + dbTableName + ":" + columnName).toUpperCase(), columnTypeTemp);
                }
            }
        }
    }

    public static AbstractColumn parseAbstractColumnToStringColumn(AbstractColumn abstractColumn) {
        return new StringColumn(abstractColumn.getColumnName(), abstractColumn.getData().toString());
    }
}
