package com.whaleal.photon;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.whaleal.photon.common.common.columntype.DbTypeFlag;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.core.programInfo.ProgramInfoUtil;
import com.whaleal.photon.common.util.Log;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 光子测试
 * 对于数据传输的校验，
 * 不管是对于源端还是目标端，我们都需要进行一系列的校验，
 * 首先需要四种数据源的连接，获取这些连接
 * oracle mysql mongo pgserver
 * 然后对这些连接进行测试
 * 建立了连接之后
 * 我们看我们到底是需要校验哪个proc的数据传输，
 * 根据proc来获取到源端和目标端的名字，然后根据名字去数据库里面获取连接，来获取它的
 * 然后我们查找源端目标表数量的总数和目标端表数量的总数
 *
 * @author cs
 * @date 2021/09/09
 */

public class PhotonTest {

    /**
     * 程序名字
     */
    private String procName = "proc2";
    //程序的信息
    private ProgramInfo programInfo;
    //数据源信息
    private Datasource sourceByDsName;
    //目标源信息
    private Datasource sourceByTargetName;

    //实际的数据库连接对象,关于mongo的
    //源连接的mongo
    private MongoClient sourceClient;
    //目标对象的mongo
    private MongoClient targetClient;
    //实际的数据库连接对象，关于关系型数据库的

    private Connection sourceConnection;
    private Connection targetConnection;

    //源表结构
    private Map<String, Object> sourceSchema = new HashMap<>();
    //目标表结构
    private Map<String, Object> targetSchema = new HashMap<>();

    private List<String> sourceTables = new ArrayList<>();
    private List<String> targetTables = new ArrayList<>();
    //源表的数量
    private Map<String, Long> sourceTableCount = new HashMap<>();
    //目标表数量
    private Map<String, Long> targetTableCount = new HashMap<>();

    @Before
    public void testBefore() {
        //在测试之前进行的操作，包括初始化一下数据源什么的
        programInfo = ProgramInfoUtil.getProgramInfo(procName);
        //程序有目标端和源端，先查询到目标端和源端的数据
        //获取源端和目标端的连接
        sourceByDsName = DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName());
        sourceByTargetName = DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName());
        //根据类型来生成连接对象  并放入到map里面，以后想要使用到去取就可以了
        createDataSourceConnection("1", sourceByDsName, sourceByTargetName);
        System.out.println(programInfo);
        System.out.println(sourceByDsName);
        System.out.println(sourceByTargetName);

        //插入数据源端的连接对象进行装填
        if (sourceByDsName.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            sourceClient = MongoDbConnection.getMongoClient(sourceByDsName.getName());
        } else if (sourceByDsName.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            sourceConnection = MySqlConnection.getConnection(sourceByDsName.getName());
        } else if (sourceByDsName.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
            sourceConnection = PgServerConnection.getConnection(sourceByDsName.getName());
        } else if (sourceByDsName.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
            sourceConnection = OracleConnection.getConnection(sourceByDsName.getName());
        }

        //接受数据目标端的对象进行装填
        if (sourceByTargetName.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            targetClient = MongoDbConnection.getMongoClient(sourceByTargetName.getName());
        } else if (sourceByTargetName.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            targetConnection = MySqlConnection.getConnection(sourceByTargetName.getName());
        } else if (sourceByTargetName.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
            targetConnection = PgServerConnection.getConnection(sourceByTargetName.getName());
        } else if (sourceByTargetName.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
            targetConnection = OracleConnection.getConnection(sourceByTargetName.getName());
        }
    }

    @Test
    public void testProc() {
        System.out.println(targetClient);
        System.out.println(sourceClient);
        System.out.println(targetConnection);
        System.out.println(sourceConnection);

        //获取连接，
        System.out.println(programInfo);
        System.out.println(sourceByDsName);
        System.out.println(sourceByTargetName);
    }

    @Test
    public void testOracle2Mongo() {

        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplate(sourceByDsName.getName());
        String sql = "select * from USER_TABLES";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("TABLESPACE_NAME").toString();
            String tableName = dbTableNameMap.get("TABLE_NAME").toString();
            String dbTable = dbSchemaName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                sourceTables.add(dbTable);
                List<Map<String, Object>> mapList = jdbcTemplate.queryForList("select t.COLUMN_NAME,t.DATA_TYPE,t.DATA_LENGTH,t.DATA_PRECISION,t.NULLABLE,t.COLUMN_ID,c.COMMENTS\n" +
                        "from user_tab_columns t, user_col_comments c where t.table_name = c.table_name and t.column_name = c.column_name and t.table_name = '" + tableName + "' order by t.COLUMN_ID");
                sourceSchema.put(dbTable,mapList);
                Map<String, Object> objectMap = jdbcTemplate.queryForMap("select count(1) from " + tableName);
                Object o = objectMap.get("COUNT(1)");
                sourceTableCount.put(dbTable, Long.parseLong(o.toString()));
            }
        }

        System.out.println("源端数据库"+sourceTables);
        System.out.println("源端表结构"+sourceSchema);
        System.out.println("源端表内数据数量"+sourceTableCount);


        MongoIterable<String> mongoIterableOfDb = targetClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = targetClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    targetTables.add(dbTable);
                    targetTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);

    }

    @Test
    public void testPg2Mongo() {
        //目标源开始进行比对
        JdbcTemplate jdbcTemplate = PgServerConnection.getJdbcTemplate(sourceByDsName.getName());

        String sql = "select  * from information_schema.TABLES where table_type='BASE TABLE' and concat(table_schema,'.',table_name)  ~ ? ";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql, programInfo.getDbTableWhite());
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("table_schema").toString();
            String tableName = dbTableNameMap.get("table_name").toString();
            String dbTable = dbSchemaName + "." + tableName;
            String schemaSql = "select * from information_schema.columns where table_schema=? and table_name=?";
            List<Map<String, Object>> maps = jdbcTemplate.queryForList(schemaSql, dbSchemaName, tableName);
            Map<String, Object> countMap = jdbcTemplate.queryForMap("select count(1) from " + dbTable);
            sourceTableCount.put(dbTable, (Long) countMap.get("count"));
            sourceSchema.put(dbTable, maps);
            sourceTables.add(dbTable);
        }
        System.out.println("源数据库" + sourceTables);
        System.out.println("表结构" + sourceSchema);
        System.out.println("表内容条数" + sourceTableCount);


        MongoIterable<String> mongoIterableOfDb = targetClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = targetClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    targetTables.add(dbTable);
                    targetTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);
    }

    @Test
    public void testMysql2Mongo() {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByDsName.getName());
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
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
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                sourceTables.add(dbTable);
            }
        }
        sourceTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = jdbcTemplate.queryForList("desc " + item);
            sourceSchema.put(item, tableDesc);
            Map<String, Object> countMap = jdbcTemplate.queryForMap("select count(1) from " + item);
            sourceTableCount.put(item, (Long) countMap.get("count(1)"));
        });

        System.out.println("数据源表" + sourceTables);
        System.out.println("数据源表结构" + sourceSchema);
        System.out.println("数据源表中数量" + sourceTableCount);

        MongoIterable<String> mongoIterableOfDb = targetClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = targetClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    targetTables.add(dbTable);
                    targetTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);
    }

    @Test
    public void testMongo2Mongo() {
        MongoIterable<String> mongSource = sourceClient.listDatabaseNames();
        MongoCursor<String> sourceCursorOfDb = mongSource.iterator();
        // 遍历库列表
        while (sourceCursorOfDb.hasNext()) {
            String dbName = sourceCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = sourceClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    sourceTables.add(dbTable);
                    sourceTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的数据源库内容");
        System.out.println(sourceTableCount);
        System.out.println(sourceTables);

        MongoIterable<String> mongoIterableOfDb = targetClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = targetClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    targetTables.add(dbTable);
                    targetTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);
    }

    @Test
    public void testMysql2Mysql() {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByDsName.getName());
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
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
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                sourceTables.add(dbTable);
            }
        }
        sourceTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = jdbcTemplate.queryForList("desc " + item);
            sourceSchema.put(item, tableDesc);
            Map<String, Object> countMap = jdbcTemplate.queryForMap("select count(1) from " + item);
            sourceTableCount.put(item, (Long) countMap.get("count(1)"));
        });

        //目标源开始进行比对
        JdbcTemplate targetJdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByTargetName.getName());
        List<Map<String, Object>> targetTableList = targetJdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
        for (Map dbTableMap : targetTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            //忽略 mysql 系统表
            if (dbName.equalsIgnoreCase("mysql") ||
                    dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();

            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                targetTables.add(dbTable);
            }
        }

        //将所有的表格的名字打印出来
        System.out.println(targetTables);
        targetTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = targetJdbcTemplate.queryForList("desc " + item);
            targetSchema.put(item, tableDesc);
            Map<String, Object> countMap = targetJdbcTemplate.queryForMap("select  count(1) from " + item);
            targetTableCount.put(item, (Long) countMap.get("count(1)"));
        });


        System.out.println("数据源表" + sourceTables);
        System.out.println("数据源表结构" + sourceSchema);
        System.out.println("数据源表中数量" + sourceTableCount);

        System.out.println("数据源表" + targetTables);
        System.out.println("数据源表结构" + targetSchema);
        System.out.println("数据源表中数量" + targetTableCount);

    }

    @Test
    public void testPg2Mysql() {
        //目标源开始进行比对
        JdbcTemplate jdbcTemplate = PgServerConnection.getJdbcTemplate(sourceByDsName.getName());

        String sql = "select  * from information_schema.TABLES where table_type='BASE TABLE' and concat(table_schema,'.',table_name)  ~ ? ";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql, programInfo.getDbTableWhite());
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("table_schema").toString();
            String tableName = dbTableNameMap.get("table_name").toString();
            String dbTable = dbSchemaName + "." + tableName;
            String schemaSql = "select * from information_schema.columns where table_schema=? and table_name=?";
            List<Map<String, Object>> maps = jdbcTemplate.queryForList(schemaSql, dbSchemaName, tableName);
            Map<String, Object> countMap = jdbcTemplate.queryForMap("select count(1) from " + dbTable);
            sourceTableCount.put(dbTable, (Long) countMap.get("count"));
            sourceSchema.put(dbTable, maps);
            sourceTables.add(dbTable);
        }
        System.out.println("源数据库" + sourceTables);
        System.out.println("表结构" + sourceSchema);
        System.out.println("表内容条数" + sourceTableCount);


        JdbcTemplate targetJdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByTargetName.getName());
        List<Map<String, Object>> dbTableList = targetJdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
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
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                targetTables.add(dbTable);
            }
        }
        System.out.println("目标表名" + targetTables);
        targetTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = targetJdbcTemplate.queryForList("desc " + item);
            targetSchema.put(item, tableDesc);
            Map<String, Object> countMap = targetJdbcTemplate.queryForMap("select count(1) from " + item);
            targetTableCount.put(item, (Long) countMap.get("count(1)"));
        });

        System.out.println("目标数据源表" + targetTables);
        System.out.println("目标数据源表结构" + targetSchema);
        System.out.println("目标数据源表中数量" + targetTableCount);

    }

    @Test
    public void testOracle2Mysql() {
        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplate(sourceByDsName.getName());
        String sql = "select * from USER_TABLES";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql);
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("TABLESPACE_NAME").toString();
            String tableName = dbTableNameMap.get("TABLE_NAME").toString();
            String dbTable = dbSchemaName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                sourceTables.add(dbTable);
                List<Map<String, Object>> mapList = jdbcTemplate.queryForList("select t.COLUMN_NAME,t.DATA_TYPE,t.DATA_LENGTH,t.DATA_PRECISION,t.NULLABLE,t.COLUMN_ID,c.COMMENTS\n" +
                        "from user_tab_columns t, user_col_comments c where t.table_name = c.table_name and t.column_name = c.column_name and t.table_name = '" + tableName + "' order by t.COLUMN_ID");
                sourceSchema.put(dbTable,mapList);
                Map<String, Object> objectMap = jdbcTemplate.queryForMap("select count(1) from " + tableName);
                Object o = objectMap.get("COUNT(1)");
                sourceTableCount.put(dbTable, Long.parseLong(o.toString()));
            }
        }

        System.out.println("源端数据库"+sourceTables);
        System.out.println("源端表结构"+sourceSchema);
        System.out.println("源端表内数据数量"+sourceTableCount);

        //目标源开始进行比对
        JdbcTemplate targetJdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByTargetName.getName());
        List<Map<String, Object>> targetTableList = targetJdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
        for (Map dbTableMap : targetTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            //忽略 mysql 系统表
            if (dbName.equalsIgnoreCase("mysql") ||
                    dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();

            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                targetTables.add(dbTable);
            }
        }

        //将所有的表格的名字打印出来
        System.out.println(targetTables);
        targetTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = targetJdbcTemplate.queryForList("desc " + item);
            targetSchema.put(item, tableDesc);
            Map<String, Object> countMap = targetJdbcTemplate.queryForMap("select  count(1) from " + item);
            targetTableCount.put(item, (Long) countMap.get("count(1)"));
        });
        System.out.println("目标数据源表" + targetTables);
        System.out.println("目标数据源表结构" + targetSchema);
        System.out.println("目标数据源表中数量" + targetTableCount);

    }

    @Test
    public void testMongo2Mysql() {

        MongoIterable<String> mongSource = sourceClient.listDatabaseNames();
        MongoCursor<String> sourceCursorOfDb = mongSource.iterator();
        // 遍历库列表
        while (sourceCursorOfDb.hasNext()) {
            String dbName = sourceCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = sourceClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    sourceTables.add(dbTable);
                    sourceTableCount.put(dbTable, count);
                }
            }
        }

        System.out.println("mongo的数据源库内容");
        System.out.println(sourceTableCount);
        System.out.println(sourceTables);



        //目标源开始进行比对
        JdbcTemplate targetJdbcTemplate = MySqlConnection.getJdbcTemplate(sourceByTargetName.getName());
        List<Map<String, Object>> targetTableList = targetJdbcTemplate.queryForList("select * from information_schema.TABLES");
        //通过语句查询到所有的表名
        for (Map dbTableMap : targetTableList) {
            String dbName = dbTableMap.get("TABLE_SCHEMA").toString();
            //忽略 mysql 系统表
            if (dbName.equalsIgnoreCase("mysql") ||
                    dbName.equalsIgnoreCase("information_schema") ||
                    dbName.equalsIgnoreCase("sys")) {
                continue;
            }
            String tableName = dbTableMap.get("TABLE_NAME").toString();

            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                targetTables.add(dbTable);
            }
        }

        //将所有的表格的名字打印出来
        System.out.println(targetTables);
        targetTables.forEach(item -> {
            List<Map<String, Object>> tableDesc = targetJdbcTemplate.queryForList("desc " + item);
            targetSchema.put(item, tableDesc);
            Map<String, Object> countMap = targetJdbcTemplate.queryForMap("select  count(1) from " + item);
            targetTableCount.put(item, (Long) countMap.get("count(1)"));
        });
        System.out.println("目标数据源表" + targetTables);
        System.out.println("目标数据源表结构" + targetSchema);
        System.out.println("目标数据源表中数量" + targetTableCount);

    }

    public static void createDataSourceConnection(String procNameAndBatchNo, Datasource... dataSourceList) {

        for (Datasource dataSourceDb : dataSourceList) {
            String procNameAndBatchNoAndDsName = dataSourceDb.getName();
            try {
                if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
                    MongoDbConnection.createMonoDbClient(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
                    MySqlConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
                    PgServerConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
                    OracleConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

}
