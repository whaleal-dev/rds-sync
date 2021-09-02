package test;

import common.dataclass.Range;
import conf.Configuration;
import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.util.ObjectUtils;
import sourcesplit.MysqlSourceSplitRange;
import sourcesplit.SingleTableSplitUtil;
import util.Log;

import java.sql.*;
import java.util.*;

/**
 * @author: jy
 * @Date: 2021/08/26
 */
public class Test {


    public static void main(String[] args) throws SQLException {
        Configuration configuration = ConfigurationUtil.getConfiguration("proc2");
        System.out.println("=========================================================================================");
        System.out.println("=======================================读取到配置如下=======================================");
        System.out.println("=========================================================================================");
        System.out.println(configuration);
        System.out.println("=========================================================================================");
        System.out.println("=========================================================================================");

        //取某列最大的 length
        Connection connection = MySqlConnection.createConnection(configuration.getSourceDsName(),
        DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(configuration.getSourceDsName());
        String table = "community.community_banner";
        String baseSql = "select * from "+ table;
        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(baseSql);
        SqlRowSetMetaData sqlRsmd = sqlRowSet.getMetaData();
        List<Map<String, String>> tableFieldList = new ArrayList<>();
        List<Long> maxList = new ArrayList<>();
        int columnCount = sqlRsmd.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            Map<String,String> fieldMap = new HashMap<>();
            fieldMap.put("fieldName", sqlRsmd.getColumnName(i));
            fieldMap.put("fieldType", String.valueOf(sqlRsmd.getColumnType(i)));
            tableFieldList.add(fieldMap);
        }
        Long result = -1L;

        String resultName = "";
        List<Map<Long, String>> maxMapList = new ArrayList<>();
        if (!tableFieldList.isEmpty()) {
            for (Map<String, String> tableField : tableFieldList) {
                String colName = tableField.get("fieldName");
                String maxlengthkey = "MAX(LENGTH(%s))";
                String maxKey = String.format(maxlengthkey, colName);
                String length = "SELECT %s FROM %s";
                String maxlengthSql = String.format(length, maxKey, table);
                List<Map<String, Object>> list = jdbcTemplate.queryForList(maxlengthSql);
                Long max = Long.parseLong(list.get(0).get(maxKey).toString());
                Map<Long, String> maxMap = new HashMap<>();
                maxMap.put(max, colName);
                maxMapList.add(maxMap);
                maxList.add(max);

                for (Long maxNum : maxList) {
                    if (maxNum >= result) {
                        result = maxNum;
                    }
                }
            }
            System.out.println("maxMapList  =   "+ maxMapList);
            for (Map<Long, String> rmap : maxMapList) {
                System.out.println("rmap.get(result) = "    + rmap.get(result));
                if (!StringUtils.isEmpty(rmap.get(result))){
                resultName = rmap.get(result);
                }
            }
            System.out.println("    最大result  =   " + result);
            System.out.println("    resultName  =   " + resultName);
            Long minresult = result;

            String minlengthkey = "MIN(LENGTH(%s))";
            String minkey = String.format(minlengthkey, resultName);
            String length = "SELECT %s FROM %s";
            String minlengthSql = String.format(length, minkey, table);
            List<Map<String, Object>> list = jdbcTemplate.queryForList(minlengthSql);
            List<Long> minlist = new ArrayList<>();
            Long min = Long.parseLong(list.get(0).get(minkey).toString());
            minlist.add(min);
            for (Long minNum : minlist) {
                if (minNum <= minresult) {
                    minresult = minNum;
                }
            }
            System.out.println("    最小result = " + minresult);
            Pair<Long, Long> pair = new ImmutablePair<Long, Long>(minresult, result);
            System.out.println("pair = " + pair);
            int splitNumber = -1;
            List<Range> lengthRange = new ArrayList<>();
            Range range = new Range();
            range.setDbTableName(table);
            range.setQuery("SELECT * FROM "+ table + " WHERE LENGTH(" + resultName + ") is NULL");
            lengthRange.add(range);

            System.out.println("lengthRange  =   " + lengthRange.get(0).getQuery());
        }
//        System.out.println("tableFieldList  =   " + tableFieldList);
//        List<Map<String, String>> longTableFieldList = new ArrayList<>();
//        List<Map<String, String>> stringTableFieldList = new ArrayList<>();
//        String sql = "SELECT MAX(LENGTH(banner_desc)) FROM community.community_banner";
//        String sql11 = "SELECT LENGTH(banner_desc) FROM community.community_banner";
//        String sql1 = "SELECT MAX(LENGTH(banner_url)) FROM community.community_banner";
//        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
//        List<Map<String, Object>> list1 = jdbcTemplate.queryForList(sql1);
//        List<Long> maxList = new ArrayList<>();
//        //取到最大值
//        Long max1 = Long.parseLong(list1.get(0).get("MAX(LENGTH(banner_url))").toString());
//        Long max = Long.parseLong(list.get(0).get("MAX(LENGTH(banner_desc))").toString());
//        System.out.println("    list    =" + list);
//        System.out.println("    max1     =" + max1);
//        System.out.println("    max     =" + max);
//        maxList.add(max);
//        maxList.add(max1);
//        //初始化默认值
//        Long result = -1L;
//        for (Long maxNum : maxList) {
//            if (maxNum >= result) {
//                result = maxNum;
//            }
//        }
//        System.out.println("result = " + result);




        //取表字段类型
//        Connection connection = MySqlConnection.createConnection(configuration.getSourceDsName(),
//                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
//        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(configuration.getSourceDsName());
////        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(
////                "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_catalog='test' AND table_name ='student' order by ordinal_position ");
////        Set<String> intColumnSet = new HashSet<>();
////        String dbTableName = "student";
////        for (Map<String, Object> columnMap : tableMeteColumn) {
////            if ("INTEGER".equalsIgnoreCase(columnMap.get("data_type").toString())) {
////                intColumnSet.add(columnMap.get("column_name").toString());
////            }
////        }
//        String tableName = "community_banner";
//        String sql = "select * from "+ tableName;
////        String sql = "SELECT banner_url FROM community.community_banner WHERE length(banner_url) != char_length(banner_url)";
//        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(sql);
//        SqlRowSetMetaData sqlRsmd = sqlRowSet.getMetaData();
//        int columnCount = sqlRsmd.getColumnCount();
//        List<Map<String, String>> longTableFieldList = new ArrayList<>();
//        List<Map<String, String>> stringTableFieldList = new ArrayList<>();
//        Map<String,String> longFieldMap = new HashMap<String,String>();
//        Map<String,String> stringFieldMap = new HashMap<String,String>();
//        String resultName = "";
//        for (int i = 1; i <= columnCount; i++) {
//            Boolean isLongType = isLongType(Integer.parseInt(String.valueOf(sqlRsmd.getColumnType(i))));
//            Boolean isStringType = isStringType(Integer.parseInt(String.valueOf(sqlRsmd.getColumnType(i))));
////            if (String.valueOf(sqlRsmd.getColumnType(i)).matches("(-5)||(4)||(-6)")) {
//            if (isLongType) {
//                longFieldMap.put("fieldName", sqlRsmd.getColumnName(i));
//                longFieldMap.put("fieldType", String.valueOf(sqlRsmd.getColumnType(i)));
//                longTableFieldList.add(longFieldMap);
//            }
//            if (isStringType) {
//                stringFieldMap.put("fieldName", sqlRsmd.getColumnName(i));
//                stringFieldMap.put("fieldType", String.valueOf(sqlRsmd.getColumnType(i)));
//                stringTableFieldList.add(stringFieldMap);
//            }
////            System.out.println("fieldMap    =   " + fieldMap.get("name"));
//        }
////        System.out.println("    " + "tableFieldList" + longTableFieldList.get(0));
//        if (!longFieldMap.isEmpty()) {
//            Pair<Object, Object> pair = SingleTableSplitUtil.getPKRange(configuration, longTableFieldList.get(0).get("fieldName"), tableName, null);
//            Long max = Long.parseLong(pair.getRight().toString());
//            String maxName = longTableFieldList.get(0).get("fieldName");
//            for (Map<String, String> tableField : longTableFieldList) {
//                System.out.println("    " + tableName + "表中为 bigint 、 int、 tinyint 的字段名为 =   " + tableField.get("fieldName"));
//                String split = tableField.get("fieldName");
//                Pair<Object, Object> minMaxPK = SingleTableSplitUtil.getPKRange(configuration, split, tableName, null);
//                System.out.println("    "+ tableField.get("fieldName") + "字段的范围为" + "minMaxPK    =   " + minMaxPK.toString());
//                System.out.println("    minMaxPK.right  =   "+ minMaxPK.getRight());
//                if ( max <= Long.parseLong(minMaxPK.getRight().toString())) {
//                    max = Long.parseLong(minMaxPK.getRight().toString());
//                    maxName = tableField.get("fieldName");
//                }
//            }
//            //得到了最大 bigint 、 int、 tinyint
//            resultName = maxName;
//            System.out.println("maxName =   " + resultName);
//        } else if (!stringFieldMap.isEmpty()) {
//            for (Map<String, String> tableField : stringTableFieldList) {
//                String colName = tableField.get("fieldName");
//                String judgeSql = "SELECT %s FROM %s WHERE length(%s) != char_length(%s)";
//                String executeSql = String.format(judgeSql, colName, tableName, colName, colName);
//                List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList(executeSql);
////                System.out.println("结果为     =   " + dbTableList);
//                if (dbTableList.isEmpty()) {
//                    System.out.println("空了=============");
//                    resultName = colName;
//                }
//            }
//            //得到了不含汉字的列名
//            System.out.println("不含汉字的列的列名为  =   " + resultName);
//            }
//        }



        // minMaxPK 最小到最大字段
//        String split = getPK(tableName, connection);
//        String split = "dict_node";
//        Pair<Object, Object> minMaxPK = SingleTableSplitUtil.getPKRange(configuration, split, tableName, null);
//        System.out.println("minMaxPK    =   " + minMaxPK.toString());

        //切表测试
/*//        configuration.setDbTableWhite("(community.community_dict)||(community.sys_menu)");
//        configuration.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.community.banner)");
        configuration.setDbTableWhite("community.test");
//        configuration.setDbTableWhite("community.community_category");
//        configuration.setDbTableWhite("community.community_banner");
//        configuration.setSplitPk("dict_node");
//        configuration.setDbTableWhite("community.sys_.*");
        configuration.setAdviceNumber(3);
//        configuration.setDbTableWhite("community.sys_captcha");
        List<Range> list = new ArrayList<>();
        try {
            System.out.println("====" + configuration);
            list = MysqlSourceSplitRange.doSplit(configuration);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
        for (Range range : list) {
            System.out.println("切分的range    =   " + range);
        }
        System.out.println("切分份数    =   " + list.size());
        System.out.println("使用的切分字段     :   " + configuration.getSplitPk());*/

        //获取主键测试
/*        Connection conn = MySqlConnection.createConnection(configuration.getSourceDsName(),
                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
        String pkName = getPK("community_banner", conn);
        System.out.println("pkName =       " + pkName);

        List<String> tables = MysqlSourceSplitRange.getDbTables(configuration);
        System.out.println("tables =  " + tables.toString());
        for (String table : tables){
            System.out.println("table =    " + table);
        }*/

        // jdbc 获取各种表信息
//        Connection conn = MySqlConnection.createConnection(configuration.getSourceDsName(),
//                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
//        System.out.println(getTables(conn));
//        System.out.println("=============================");
//        Statement st = conn.createStatement();
//        ResultSet rs = st.executeQuery("select * from community_banner");
//        List<String> colNames = getColNames(rs);
//        while(rs.next()){
//            for (int i = 0; i < colNames.size(); i++) {
//                System.out.print(rs.getObject(colNames.get(i)));
//                if (i!=colNames.size()-1){
//                    System.out.print("\t");
//                }
//            }
//            System.out.println();
//        }
//        rs.close();
//        st.close();
//        conn.close();
    }









//        List<Range> listRange = ReaderSplitUtil.doSplit(configuration, configuration.getAdviceNumber(),
//                ReaderSplitUtil.getTableNumber(configuration));
//        System.out.println("listRange =  " + listRange.toString());
//        for (Range splitRange : listRange){
//            System.out.println("splitRange =    " + splitRange);
//        }


//        test1(configuration);
//        List<Configuration> listConf = ReaderSplitUtil.doSplit(configuration, configuration.getInt("adviceNumber", 2));
//        Configuration splitConf = listConf.get(1);
//        MysqlSourceTaskInfo taskMetadata = MysqlSourceTaskInfo.builder().rangeSql(splitConf.getString(Key.QUERY_SQL))
//                .sourceUrl(splitConf.getString(Key.JDBC_URL)).databaseType(splitConf.getString(Key.DATABASE_TYPE))
//                .sourceUsername(splitConf.getString(Key.USERNAME)).sourcePassword(splitConf.getString(Key.PASSWORD))
//                .sourceDatabase(splitConf.getString(Key.DATABASE)).sourceTable(splitConf.getString(Key.TABLE))
//                .targetUrl(splitConf.getString("target.jdbcUrl")).targetUsername(splitConf.getString("target.username", null))
//                .targetPassword(splitConf.getString("target.password", null)).targetDatabase(splitConf.getString(Key.DATABASE))
//                .targetCollection(StringUtils.strip(splitConf.getString("target.collection"), "[]").replaceAll("\"", "")).dataBatchSize(splitConf.getInt("dataBatchSize"))
//                .build();
//
//        System.out.println("taskMetadata =      " + taskMetadata);
//        getDataFromCollection(configuration);
//        getAllDbTables(configuration);


    private static boolean isLongType(int type) {
        boolean isValidLongType = type == Types.BIGINT || type == Types.INTEGER
                || type == Types.SMALLINT || type == Types.TINYINT;

        return isValidLongType;
    }

    private static boolean isStringType(int type) {
        return type == Types.CHAR || type == Types.NCHAR
                || type == Types.VARCHAR || type == Types.LONGVARCHAR
                || type == Types.NVARCHAR;
    }

    public static String getPK(String tableName, Connection conn) {
        String PKName = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet rs = dmd.getPrimaryKeys(null, "%", tableName);
            rs.next();
            PKName = rs.getString("column_name");
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return PKName;
    }

    public static void test1(Configuration conf) throws SQLException {
//        List<JSONObject> connConfList = conf.getList(Key.CONNECTION, JSONObject.class);
//        Connection conn = DBUtil.getConnection(conf);
//        DatabaseMetaData metaData = conn.getMetaData();
//        String[] types = {"TABLE"};
//        ResultSet rs = metaData.getTables(null, null, "%", types);
//        Map<String, String> dbTables = new HashMap<>();
//        String dbTableWhite = "sys_menu.";
//        while(rs.next()){
//            //1 TABLE_CAT String => table catalog (may be null)
//            //2 TABLE_SCHEM String => table schema (may be null)
//            //3 TABLE_NAME String => table name
//            String tableName = rs.getString(3);
//            System.out.println("tableName = " + tableName);
//            //TODO
//            String dbName = rs.getString(2);
//            System.out.println("dbName = " + dbName);
//            //读取配置中的table
//            //TODO 表从哪来
////            String tableConf = StringUtils.strip(connConfList.get(0).getString(Key.TABLE), "[]")
////                    .replaceAll("\"", "");
//            String dbTable = dbName + "." + tableName;
//            if (dbTable.matches(dbTableWhite)) {
//                dbTables.put(dbTable, dbTable);
//            }
//        }
//        rs.close();
//        Log.info("sourceName:  " + conf.getString("database") + ",全量同步的表列表:  " + dbTables);
    }


    //        //方法二
//        String sql = String.format("show index from %s", tableName);
//        try {
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ResultSet rs = ps.executeQuery();
//            rs.next();
//            PKName = rs.getString("column_name");
//            rs.close();
//            ps.close();
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }
    public static void getAllDbTables(Configuration conf) throws SQLException {
//        List<JSONObject> connConfList = conf.getList("connection", JSONObject.class);
//        String jdbcUrl = connConfList.get(3).getString(Key.JDBC_URL);
//        Connection conn = DBUtil.getConnection(conf);
////        Connection conn = DBUtil.getConnection(DataBaseType.MySql, jdbcUrl,
////                conf.getString(Key.USERNAME), conf.getString(Key.PASSWORD));
//        DatabaseMetaData metaData = conn.getMetaData();
//
//        String[] types = {"TABLE"};
//        ResultSet rs = metaData.getTables(null, null, "%", types);
//        Map<String, String> dbTables = new HashMap<>();
//        while(rs.next()){
//            //1 TABLE_CAT String => table catalog (may be null)
//            //2 TABLE_SCHEM String => table schema (may be null)
//            //3 TABLE_NAME String => table name
//            String table = rs.getString(3);
//            //读取配置中的table
//            String tableconf = StringUtils.strip(connConfList.get(0).getString("table"), "[]")
//                    .replaceAll("\"", "");
//            if (table.equals(tableconf)) {
//                dbTables.put(table, tableconf);
//            }
//        }
//        rs.close();
//        Log.info("sourceName:  " + conf.getString("database") + ",全量同步的表列表:  " + dbTables);
    }

    public static void getDataFromCollection(Configuration conf) {
//
//        MysqlSourceTaskInfo taskMetadata = new MysqlSourceTaskInfo();
//        List<Configuration> listConf = ReaderSplitUtil.doSplit(conf, conf.getInt("adviceNumber", 2));
//        Configuration splitConf = listConf.get(1);
//
//        String rangeSql = splitConf.getString(Key.QUERY_SQL);
//        String sourceUrl = splitConf.getString(Key.JDBC_URL);
//        String databaseType = splitConf.getString(Key.DATABASE_TYPE);
//        String sourceUsername = splitConf.getString(Key.USERNAME);
//        String sourcePassword = splitConf.getString(Key.PASSWORD);
//        String sourceDatabase = splitConf.getString("database");
//        String soureTable = splitConf.getString(Key.TABLE);
//        String targetUrl = splitConf.getString("target.jdbcUrl");
//        String targetUsername = splitConf.getString("target.username", null);
//        String targetPassword = splitConf.getString("target.password", null);
//        String targetDatabase = splitConf.getString("target.database");
//        String targetCollection = splitConf.getString("target.collection");
//        Integer dataBatchSize = splitConf.getInt("dataBatchSize");
//
//        taskMetadata.setRangeSql(rangeSql);
//        taskMetadata.setSourceUrl(sourceUrl);
//        taskMetadata.setDatabaseType(databaseType);
//        taskMetadata.setSourceUsername(sourceUsername);
//        taskMetadata.setSourcePassword(sourcePassword);
//        taskMetadata.setSourceDatabase(sourceDatabase);
//        taskMetadata.setSourceTable(soureTable);
//        taskMetadata.setTargetUrl(targetUrl);
//        taskMetadata.setTargetUsername(targetUsername);
//        taskMetadata.setTargetPassword(targetPassword);
//        taskMetadata.setTargetDatabase(targetDatabase);
//        taskMetadata.setTargetCollection(targetCollection);
//        taskMetadata.setDataBatchSize(dataBatchSize);
//        Log.info("启动source任务:" + taskMetadata.toString());
//
//        String sql = taskMetadata.getRangeSql();
//        System.out.println("SQL语句为：     " + sql);
//        Connection conn = DBUtil.getConnection(taskMetadata);
//        DataUtil dataUtil = new DataUtil(conn);
////        List<AbstractColumn> abstractColumns = data.getAbstractColumns(sql);
//        //获取数据
//        List<List<AbstractColumn>> dataList = dataUtil.getMysqlDatalist(sql);
//
////        System.out.println("列名 =     " + abstractColumns.getColumnName() + "值 =      " + abstractColumns.get(0).getData());
////        dataList.add(abstractColumns);
//        System.out.println("dataList    =    "  + dataList);
//        Log.info("source任务查询完毕:" + taskMetadata.toString());
    }

//    public static void dataTransformation(Object document) {
//        List<List<AbstractColumn>> dataList = new ArrayList<>();
//        List<AbstractColumn> abstractColumns = new ArrayList<>();
//        Iterator<Map.Entry<String, Object>> iterator = ((Document) document).entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<String, Object> next = iterator.next();
//            AbstractColumn abstractColumn = TransformationMongodbDataToColumn.parseValue(next.getKey(), next.getValue());
//            abstractColumns.add(abstractColumn);
//        }
//        dataList.add(abstractColumns);
//    }
    /**获取数据库中所有表名称
     * @param conn
     * @return
     * @throws SQLException
     */
    private static List<String> getTables(Connection conn) throws SQLException {
        DatabaseMetaData databaseMetaData = conn.getMetaData();
        ResultSet tables = databaseMetaData.getTables(null, null, "%", null);
        ArrayList<String> tablesList = new ArrayList<String>();
        while (tables.next()) {
            tablesList.add(tables.getString("TABLE_NAME"));
        }
        return tablesList;
    }

    /**获取表中所有字段名称
     * @param rs
     * @throws SQLException
     */
    private static List<String> getColNames(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int count = metaData.getColumnCount();
        System.out.println("getCatalogName(int column) 获取指定列的表目录名称。"+metaData.getCatalogName(1));
        System.out.println("getColumnClassName(int column) 构造其实例的 Java 类的完全限定名称。"+metaData.getColumnClassName(1));
        System.out.println("getColumnCount()  返回此 ResultSet 对象中的列数。"+metaData.getColumnCount());
        System.out.println("getColumnDisplaySize(int column) 指示指定列的最大标准宽度，以字符为单位. "+metaData.getColumnDisplaySize(1));
        System.out.println("getColumnLabel(int column) 获取用于打印输出和显示的指定列的建议标题。 "+metaData.getColumnLabel(1));
        System.out.println("getColumnName(int column)  获取指定列的名称。"+metaData.getColumnName(1));
        System.out.println("getColumnType(int column) 获取指定列的 SQL 类型。 "+metaData.getColumnType(1));
        System.out.println("getColumnTypeName(int column) 获取指定列的数据库特定的类型名称。 "+metaData.getColumnTypeName(1));
        System.out.println("getPrecision(int column)  获取指定列的指定列宽。 "+metaData.getPrecision(1));
        System.out.println("getScale(int column) 获取指定列的小数点右边的位数。 "+metaData.getScale(1));
        System.out.println("getSchemaName(int column) 获取指定列的表模式。 "+metaData.getSchemaName(1));
        System.out.println("getTableName(int column) 获取指定列的名称。 "+metaData.getTableName(1));
        List<String> colNameList = new ArrayList<String>();
        for(int i = 1; i<=count; i++){
            colNameList.add(metaData.getColumnName(i));
        }
        System.out.println(colNameList);
//		rs.close();
        rs.first();
        return colNameList;
    }
}

