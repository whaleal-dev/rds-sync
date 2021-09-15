package com.whaleal.photon;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;

import com.whaleal.photon.common.common.columntype.DbTypeFlag;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import com.whaleal.photon.core.programInfo.ProgramInfoUtil;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;


import java.sql.Connection;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
    private String procName = "proc1";

    private Map<String, Object> sourcePrimaryKey = new HashMap<>();

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
    //目标源抽取的案例数据
    private Map<String, List<Map<String, Object>>> sourceSampleDataList = new HashMap<>();

    private Map<String, List<Map<String, Object>>> targetSampleDataList = new HashMap<>();

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
    public void testOracle2Mongo() {

        oracleSourceDataInject();
        System.out.println("oracle的目标源库内容");
        System.out.println("源端数据库" + sourceTables);
        System.out.println("源端表结构" + sourceSchema);
        System.out.println("源端表内数据数量" + sourceTableCount);

        mongoTargetDataInject();

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);
        System.out.println("源端和目标端表的数量对比");

        System.out.println(targetTableCount.equals(sourceTableCount));

    }

    @Test
    public void testPg2Mongo() {
        pgSourceDataInject();

        System.out.println("源数据库" + sourceTables);
        System.out.println("表结构" + sourceSchema);
        System.out.println("表内容条数" + sourceTableCount);


        mongoTargetDataInject();

        System.out.println("mongo的目标源库内容");
        System.out.println(targetTableCount);
        System.out.println(targetTables);
    }

    @Test
    public void testMysql2Mongo() {
        mysqlSourceDataInject();

        mongoTargetDataInject();

        checkTable();

        checkContentCount();

        checkData();

    }


    @Test
    public void testMongo2Mongo() {
        mongoSourceDataInject();

        mongoTargetDataInject();

        checkTable();

        checkContentCount();

        checkData();
    }


    @Test
    public void testMysql2Mysql() {

        mysqlSourceDataInject();
        mysqlTargetDataInject();

        System.out.println("数据源表" + sourceTables);
        System.out.println("数据源表结构" + sourceSchema);
        System.out.println("数据源表中数量" + sourceTableCount);

        System.out.println("数据源表" + targetTables);
        System.out.println("数据源表结构" + targetSchema);
        System.out.println("数据源表中数量" + targetTableCount);

    }

    @Test
    public void testPg2Mysql() {
        pgSourceDataInject();
        mysqlTargetDataInject();

        checkTable();
        checkContentCount();
        checkData();
    }

    @Test
    public void testOracle2Mysql() {
        oracleSourceDataInject();

        mysqlTargetDataInject();
        checkTable();
        checkContentCount();
        checkData();
    }

    @Test
    public void testMongo2Mysql() {
        mongoSourceDataInject();
        System.out.println("mongo的数据源库内容");
        System.out.println(sourceTableCount);
        System.out.println(sourceTables);

        mysqlTargetDataInject();
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

    public void mysqlTargetDataInject() {
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
            //其中一个表
            List<Map<String, Object>> targetList = new ArrayList<>();
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                targetTables.add(dbTable);
                List<Map<String, Object>> tableDesc = targetJdbcTemplate.queryForList("desc " + dbTable);
                targetSchema.put(dbTable, tableDesc);
                Map<String, Object> countMap = targetJdbcTemplate.queryForMap("select  count(1) from " + dbTable);
                targetTableCount.put(dbTable, (Long) countMap.get("count(1)"));
                String querySql = "select * from " + dbTable + " where %s = %s";

                //根据源表抽象数据查询表中的抽样数据， 开始
                if (sourceSampleDataList.get(dbTable) != null) {
                    //源数据表里面的内容
                    List<Map<String, Object>> dataList = sourceSampleDataList.get(dbTable);
                    //此表对应的主键
                    String primaryKey = String.valueOf(sourcePrimaryKey.get(dbTable));
                    //一条数据记录
                    dataList.forEach(item -> {
                        //数据记录里面有很多内容，列的名字，和列的值
                        //测试数据的主键值
                        Object primaryValue = item.get(primaryKey);
                        String querySqlResult = String.format(querySql, primaryKey, primaryValue);

                        System.out.println(querySql);
                        System.out.println(querySqlResult);
                        Map<String, Object> targetData = new HashMap<>();
                        try {
                            targetData = targetJdbcTemplate.queryForMap(querySqlResult);
                            System.out.println("查询到了数据" + targetData);
                        } catch (Exception e) {
                            targetData = null;
                            System.out.println("根据源端案例数据查询到目标端对应数据失败，数据查询语句为：" + querySqlResult);

                        }
                        if (targetData != null) {
                            targetList.add(targetData);
                        }
                    });
                    //根据原本的数据查找完成
                    targetSampleDataList.put(dbTable, targetList);
                }

            }
        }

        System.out.println(targetSampleDataList);

    }

    public void oracleSourceDataInject() {
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
                sourceSchema.put(dbTable, mapList);
                Map<String, Object> objectMap = jdbcTemplate.queryForMap("select count(1) from " + tableName);
                Object o = objectMap.get("COUNT(1)");
                sourceTableCount.put(dbTable, Long.parseLong(o.toString()));
                String findPrimaryKeySql = "select a.constraint_name,  a.column_name from user_cons_columns a, user_constraints b where a.constraint_name = b.constraint_name and b.constraint_type = 'P' and a.table_name = '" + tableName + "'";
                Map<String, Object> primaryKeyMap = jdbcTemplate.queryForMap(findPrimaryKeySql);
                sourcePrimaryKey.put(dbTable, primaryKeyMap.get("COLUMN_NAME"));
                List<Map<String, Object>> sampleDataList = jdbcTemplate.queryForList("select * from (select  *  from " + tableName + " order by dbms_random.value ) where rownum <= 6");
                sourceSampleDataList.put(dbTable, sampleDataList);
            }
        }
    }

    public void pgSourceDataInject() {
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
            //查找主键
            String findPrimaryKeySql = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema='" + dbSchemaName + "' AND table_name ='" + tableName + "' order by ordinal_position LIMIT 1";
            Map<String, Object> primaryKeyMap = jdbcTemplate.queryForMap(findPrimaryKeySql);
            Object o = primaryKeyMap.get("column_name");
            sourcePrimaryKey.put(dbTable, o);
            //随机取出数据
            String querySql = "select * from " + dbTable + " order by random() limit 3";
            List<Map<String, Object>> sampleDataList = jdbcTemplate.queryForList(querySql);
            sourceSampleDataList.put(dbTable, sampleDataList);
        }
    }

    public void mysqlSourceDataInject() {
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
                List<Map<String, Object>> tableDesc = jdbcTemplate.queryForList("desc " + dbTable);
                sourceSchema.put(dbTable, tableDesc);
                Map<String, Object> countMap = jdbcTemplate.queryForMap("select count(1) from " + dbTable);
                sourceTableCount.put(dbTable, (Long) countMap.get("count(1)"));
                //查询这个表的主键
                String findPrimaryKeySql = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema='" + dbName + "' AND t.table_name ='" + tableName + "' order by ordinal_position LIMIT 1";
                Map<String, Object> primaryKeyMap = jdbcTemplate.queryForMap(findPrimaryKeySql);
                Object o = primaryKeyMap.get("COLUMN_NAME");
                sourcePrimaryKey.put(dbTable, o);
                //随机取出数据
                String querySql = "SELECT * FROM `" + tableName + "`WHERE " + o + " >= (SELECT floor(RAND() * (SELECT MAX(" + o + ") FROM `" + tableName + "`))) ORDER BY " + o + " LIMIT 3";
                List<Map<String, Object>> sampleDataList = jdbcTemplate.queryForList(querySql);
                sourceSampleDataList.put(dbTable, sampleDataList);
            }
        }
    }

    public void mongoTargetDataInject() {
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
                    //根据源端的数据来查询目标端的数据
                    //根据源表抽象数据查询表中的抽样数据， 开始
                    if (sourceSampleDataList.get(dbTable) != null) {
                        //源数据表里面的内容
                        List<Map<String, Object>> dataList = sourceSampleDataList.get(dbTable);
                        //此表对应的主键
                        String primaryKey = String.valueOf(sourcePrimaryKey.get(dbTable));
                        //一条数据记录 TODO
                        List<Map<String, Object>> targetDataList = new ArrayList<>();
                        dataList.forEach(item -> {
                            //数据记录里面有很多内容，列的名字，和列的值
                            //测试数据的主键值
                            Object primaryValue = item.get(primaryKey);
                            BasicDBObject condition = new BasicDBObject();
                            // 不要紧在where条件中单独添加type的查询
                            condition.append(primaryKey, primaryValue);
                            Document document = null;
                            try {
                                document = targetClient.getDatabase(dbName).getCollection(tableName).find(condition).first();
                            } catch (Exception e) {
                                System.out.println("查询目标数据失败,查询条件" + dbTable + primaryKey + primaryValue);
                            }
                            if (document != null) {
                                targetDataList.add(document);
                            }
                        });

                        //根据原本的数据查找完成
                        targetSampleDataList.put(dbTable, targetDataList);
                    }

                }
            }
        }
    }

    public void mongoSourceDataInject() {
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
                List<Map<String, Object>> targetList = new ArrayList<>();
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(programInfo.getDbTableWhite())) {
                    Long count = targetClient.getDatabase(dbName).getCollection(tableName).countDocuments();
                    sourceTables.add(dbTable);
                    //mongo的主键都是  _id
                    sourceTableCount.put(dbTable, count);
                    sourcePrimaryKey.put(dbTable, "_id");
                    FindIterable<Document> documents = targetClient.getDatabase(dbName).getCollection(tableName).find().limit(3);
                    MongoCursor<Document> iterator = documents.iterator();
                    while (iterator.hasNext()) {
                        targetList.add(iterator.next());
                    }
                }
                sourceSampleDataList.put(dbTable, targetList);
            }
        }
    }

    /**
     * 检查表
     */
    private void checkTable() {
        System.out.println("数据源表" + sourceTables);
        System.out.println("目标源表" + targetTables);
        System.out.println("数据源表和目标表数量的对比");
        //找到公共的表
        List<String> common = targetTables.stream().filter(sourceTables::contains).collect(toList());
        if (common.size() < sourceTables.size()) {
            String format = String.format("目标端表的数量和源端表数量不一致，目标端表端数量为：%d ，源端表的数量为：%d ，缺少了%d 张表的数据", targetTables.size(), sourceTables.size(), (sourceTables.size() - common.size()));
            System.out.println(format);
            List<String> reduce1 = sourceTables.stream().filter(item -> !common.contains(item)).collect(toList());
            System.out.println("差异的表名字为" + reduce1);
        } else {
            System.out.println("目标端表的数量和源端表数量一致");
        }
    }

    /**
     * 对比源和目标的表中字段数量
     */
    private void checkContentCount() {
        //对比源和目标的表中字段数量 begin
        Iterator<Map.Entry<String, Long>> sourceIterator = sourceTableCount.entrySet().iterator();
        while (sourceIterator.hasNext()) {
            Map.Entry<String, Long> next = sourceIterator.next();
            String key = next.getKey();
            Long value = next.getValue();
            if (targetTableCount.get(key) == null) {
                System.out.println("目标端没有表" + key);
            } else if (!value.equals(targetTableCount.get(key))) {
                String format = String.format("表数量存在差异,源端表：%s ，表内容条数：%d ； 目标端表：%s  ，表内容条数：%d 。 ", key, value, key, targetTableCount.get(key));
                System.out.println(format);
            } else {
                String format = String.format("表数量没有差异,源端表：%s ，表内容条数：%d ； 目标端表：%s  ，表内容条数：%d 。 ", key, value, key, targetTableCount.get(key));
                System.out.println(format);
            }
        }
        //对比源和目标的表中字段数量 end
    }

    /**
     * 对比数据内容
     */
    private void checkData() {
        System.out.println("开始校对数据");
        //对比测试数据 begin
        Iterator<Map.Entry<String, List<Map<String, Object>>>> iterator = targetSampleDataList.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Map<String, Object>>> entry = iterator.next();
            String tableName = entry.getKey();
            List<Map<String, Object>> dataList = entry.getValue();
            dataList.forEach(item -> {
                Object o = sourcePrimaryKey.get(tableName);
                Stream<Map<String, Object>> mapStream = sourceSampleDataList.get(tableName).stream().filter(
                        item2 -> item2.get(sourcePrimaryKey.get(tableName)).equals(item.get(sourcePrimaryKey.get(tableName))));
                //收集到的源端的数据
                Map<String, Object> collect = mapStream.collect(toList()).get(0);
                //目前item就是目标端的数据， collect就是我们根据目标端查询到的源端的数据
                Iterator<Map.Entry<String, Object>> sourceItemIterator = collect.entrySet().iterator();
                while (sourceItemIterator.hasNext()) {
                    Map.Entry<String, Object> next = sourceItemIterator.next();
                    Object sourceValue = item.get(next.getKey());

                    if (next.getValue() != null && !next.getValue().toString().equals(sourceValue.toString())) {
                        System.out.println(tableName + "表的" + next.getKey() + "字段传输不一致,源端值为" + next.getValue() + "目标端获取到的数据为" + sourceValue);
                    }
                }
            });
        }
        System.out.println("校对完成");
        //对比测试数据 end
    }
}
