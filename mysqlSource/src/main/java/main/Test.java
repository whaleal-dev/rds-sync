package main;

import com.alibaba.fastjson.JSONObject;
import common.column.AbstractColumn;
import common.taskbase.metadata.SourceTaskInfo1;
import conf.Configuration;
import conf.DBUtil;
import conf.DataUtil;
import conf.ReaderSplitUtil;
import constant.Key;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import parse.TransformationMongodbDataToColumn;
import util.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author: jy
 * @Date: 2021/08/26
 */
public class Test {


    public static void main(String[] args) throws SQLException {
        File file = new File("/Users/cs/Documents/3.json");
        Configuration configuration = Configuration.from(file);
        System.out.println("=========================================================================================");
        System.out.println("=========================        =======读取到配置如下==========  =   ======================");
        System.out.println("=========================================================================================");
        System.out.println(configuration);
        System.out.println("=========================================================================================");
        List<Configuration> listConf = ReaderSplitUtil.doSplit(configuration, configuration.getInt("adviceNumber", 2));
        Configuration splitConf = listConf.get(1);
        SourceTaskInfo1 taskMetadata = SourceTaskInfo1.builder().rangeSql(splitConf.getString(Key.QUERY_SQL))
                .sourceUrl(splitConf.getString(Key.JDBC_URL)).databaseType(splitConf.getString(Key.DATABASE_TYPE))
                .sourceUsername(splitConf.getString(Key.USERNAME)).sourcePassword(splitConf.getString(Key.PASSWORD))
                .sourceDatabase(splitConf.getString(Key.DATABASE)).sourceTable(splitConf.getString(Key.TABLE))
                .targetUrl(splitConf.getString("target.jdbcUrl")).targetUsername(splitConf.getString("target.username", null))
                .targetPassword(splitConf.getString("target.password", null)).targetDatabase(splitConf.getString(Key.DATABASE))
                .targetCollection(StringUtils.strip(splitConf.getString("target.collection"), "[]").replaceAll("\"", "")).dataBatchSize(splitConf.getInt("dataBatchSize"))
                .build();

        System.out.println("taskMetadata =      " + taskMetadata);
        getDataFromCollection(configuration);
//        getAllDbTables(configuration);
    }

    public static void getAllDbTables(Configuration conf) throws SQLException {
        List<JSONObject> connConfList = conf.getList("connection", JSONObject.class);
        String jdbcUrl = connConfList.get(3).getString(Key.JDBC_URL);
        Connection conn = DBUtil.getConnection(conf);
//        Connection conn = DBUtil.getConnection(DataBaseType.MySql, jdbcUrl,
//                conf.getString(Key.USERNAME), conf.getString(Key.PASSWORD));
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
            String tableconf = StringUtils.strip(connConfList.get(0).getString("table"), "[]")
                    .replaceAll("\"", "");
            if (table.equals(tableconf)) {
                dbTables.put(table, tableconf);
            }
        }
        rs.close();
        Log.info("sourceName:  " + conf.getString("database") + ",全量同步的表列表:  " + dbTables);
    }

    public static void getDataFromCollection(Configuration conf){
        SourceTaskInfo1 taskMetadata = new SourceTaskInfo1();
        List<Configuration> listConf = ReaderSplitUtil.doSplit(conf, conf.getInt("adviceNumber", 2));
        Configuration splitConf = listConf.get(1);

        String rangeSql = splitConf.getString(Key.QUERY_SQL);
        String sourceUrl = splitConf.getString(Key.JDBC_URL);
        String databaseType = splitConf.getString(Key.DATABASE_TYPE);
        String sourceUsername = splitConf.getString(Key.USERNAME);
        String sourcePassword = splitConf.getString(Key.PASSWORD);
        String sourceDatabase = splitConf.getString("database");
        String soureTable = splitConf.getString(Key.TABLE);
        String targetUrl = splitConf.getString("target.jdbcUrl");
        String targetUsername = splitConf.getString("target.username", null);
        String targetPassword = splitConf.getString("target.password", null);
        String targetDatabase = splitConf.getString("target.database");
        String targetCollection = splitConf.getString("target.collection");
        Integer dataBatchSize = splitConf.getInt("dataBatchSize");

        taskMetadata.setRangeSql(rangeSql);
        taskMetadata.setSourceUrl(sourceUrl);
        taskMetadata.setDatabaseType(databaseType);
        taskMetadata.setSourceUsername(sourceUsername);
        taskMetadata.setSourcePassword(sourcePassword);
        taskMetadata.setSourceDatabase(sourceDatabase);
        taskMetadata.setSourceTable(soureTable);
        taskMetadata.setTargetUrl(targetUrl);
        taskMetadata.setTargetUsername(targetUsername);
        taskMetadata.setTargetPassword(targetPassword);
        taskMetadata.setTargetDatabase(targetDatabase);
        taskMetadata.setTargetCollection(targetCollection);
        taskMetadata.setDataBatchSize(dataBatchSize);
        Log.info("启动source任务:" + taskMetadata.toString());

        String sql = taskMetadata.getRangeSql();
        System.out.println("SQL语句为：     " + sql);
        Connection conn = DBUtil.getConnection(taskMetadata);
        DataUtil dataUtil = new DataUtil(conn);
//        List<AbstractColumn> abstractColumns = data.getAbstractColumns(sql);
        //获取数据
        List<List<AbstractColumn>> dataList = dataUtil.getMysqlDatalist(sql);

//        System.out.println("列名 =     " + abstractColumns.getColumnName() + "值 =      " + abstractColumns.get(0).getData());
//        dataList.add(abstractColumns);
        System.out.println("dataList    =    "  + dataList);
        Log.info("source任务查询完毕:" + taskMetadata.toString());
    }

    public static void dataTransformation(Object document) {
        List<List<AbstractColumn>> dataList = new ArrayList<>();
        List<AbstractColumn> abstractColumns = new ArrayList<>();
        Iterator<Map.Entry<String, Object>> iterator = ((Document) document).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            AbstractColumn abstractColumn = TransformationMongodbDataToColumn.parseValue(next.getKey(), next.getValue());
            abstractColumns.add(abstractColumn);
        }
        dataList.add(abstractColumns);
    }


}