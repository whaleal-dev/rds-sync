package sourcesplit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.dataclass.Range;
import common.photonV.entity.ProgramInfo;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import util.Log;
import util.split.RangeSplitWrap;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;


public class SingleTableSplitUtil {

    public static List<Range> splitSingleTable(ProgramInfo programInfo, String table, int adviceNum) {

        List<Range> pluginParams = new ArrayList<Range>();
        List<String> rangeList = null;
        //从配置中取分片字段 splitPk
        String splitPkName = null;
        String sql = "SELECT * FROM " + table;
//        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(programInfo.getSourceDsName());
//        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList(sql);
//        if (dbTableList.isEmpty()) {
//            return pluginParams;
//        }
        if (StringUtils.isNotBlank(programInfo.getSplitPk())) {
            splitPkName = programInfo.getSplitPk();
        } else {
            splitPkName = getResultPK(programInfo, table);
        }
        if (splitPkName.equals("有汉字列")) {
            return splitChineseTable(programInfo, table, adviceNum);
        }
        //没有可用主键
        if (StringUtils.isEmpty(splitPkName)) {
            Range range = new Range();
            range.setDbTableName(table);
            range.setQuery("SELECT * FROM " + table);
            pluginParams.add(range);
            return pluginParams;
        }
        Log.info("      " + table + "   表使用了    \"" + splitPkName + "\"     字段切分   ");
        String column = "*";
        String where = null;
        //配置中有无where
        boolean hasWhere = StringUtils.isNotBlank(where);
        // Pair Java中的配对
        // minMaxPK 最小到最大字段
        Pair<Object, Object> minMaxPK = getPkRange(programInfo, table, where);
        if (null == minMaxPK) {
            Log.error("根据切分主键切分表失败. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
        }
        Range range = new Range();
        range.setDbTableName(table);
        range.setQuery(buildQuerySql(column, table, where));

        // 切分后获取到的 start/end 有 Null 的情况
        if (null == minMaxPK.getLeft() || null == minMaxPK.getRight()) {
            pluginParams.add(range);
            return pluginParams;
        }

        boolean isStringType = "pkTypeString".equals(programInfo
                .getPK_TYPE());
        boolean isLongType = "pkTypeLong".equals(programInfo
                .getPK_TYPE());

        if (isStringType) {
            rangeList = RangeSplitWrap.splitAndWrap(
                    String.valueOf(minMaxPK.getLeft()),
                    String.valueOf(minMaxPK.getRight()), adviceNum,
                    splitPkName, "'");
        } else if (isLongType) {
            rangeList = RangeSplitWrap.splitAndWrap(
                    new BigInteger(minMaxPK.getLeft().toString()),
                    new BigInteger(minMaxPK.getRight().toString()),
                    adviceNum, splitPkName);
        } else {
            Log.error("您配置的切分主键(splitPk) 类型 PhotonT 不支持. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
        }

        String tempQuerySql;
        List<String> allQuerySql = new ArrayList<String>();

        if (null != rangeList && !rangeList.isEmpty()) {
            for (String range1 : rangeList) {
                tempQuerySql = buildQuerySql(column, table, where)
                        + (hasWhere ? " and " : " where ") + range1;
                allQuerySql.add(tempQuerySql);
                range = new Range();
                range.setDbTableName(table);
                range.setQuery(tempQuerySql);
                pluginParams.add(range);
            }
        } else {
            //pluginParams.add(programInfo); // this is wrong for new & old split
            tempQuerySql = buildQuerySql(column, table, where)
                    + (hasWhere ? " and " : " where ")
                    + String.format(" %s IS NOT NULL", splitPkName);
            allQuerySql.add(tempQuerySql);
            range = new Range();
            range.setDbTableName(table);
            range.setQuery(tempQuerySql);
            pluginParams.add(range);
        }

        // deal pk is null
        tempQuerySql = buildQuerySql(column, table, where)
                + (hasWhere ? " and " : " where ")
                + String.format(" %s IS NULL", splitPkName);
        allQuerySql.add(tempQuerySql);
        range = new Range();
        range.setDbTableName(table);
        range.setQuery(tempQuerySql);
        pluginParams.add(range);

        return pluginParams;
    }

    public static String buildQuerySql(String column, String table, String where) {
        String querySql;
        if (StringUtils.isBlank(where)) {
            querySql = String.format("select %s from %s ",
                    column, table);
        } else {
            querySql = String.format("select %s from %s where (%s)", column,
                    table, where);
        }
        return querySql;
    }

    @SuppressWarnings("resource")
    private static Pair<Object, Object> getPkRange(ProgramInfo programInfo, String table, String where) {
        //字段构建的范围 sql pkRangeSQL
        String pkRangeSQL = genPkRangeSQL(programInfo, table, where);
        //取配置中的 fetchSize
        int fetchSize = programInfo.getFetchSize();
        //获取连接
        String procNameAndBatchNoAndSourceDsName = programInfo.getProName() + programInfo.getBatchNO()+programInfo.getSourceDsName();
        Connection conn = MySqlConnection.getConnection(procNameAndBatchNoAndSourceDsName);
        //字段构建的范围 sql pkRangeSQL
        Pair<Object, Object> minMaxPK = checkSplitPk(conn, pkRangeSQL, fetchSize, programInfo);

        return minMaxPK;
    }

    public static Pair<Object, Object> getPKRange(ProgramInfo programInfo, String split, String table, String where) {

        //字段构建的范围 sql pkRangeSQL
        String pkRangeSQL = genPKSql(split, table, where);
        //取配置中的 fetchSize
        int fetchSize = programInfo.getFetchSize();
        //获取连接
        String procNameAndBatchNoAndSourceDsName = programInfo.getProName() + programInfo.getBatchNO()+programInfo.getSourceDsName();
        Connection conn = MySqlConnection.getConnection(procNameAndBatchNoAndSourceDsName);
        //字段构建的范围 sql pkRangeSQL
        Pair<Object, Object> minMaxPK = checkSplitPk(conn, pkRangeSQL, fetchSize, programInfo);

        return minMaxPK;
    }

    /**
     * 检测splitPk的配置是否正确。
     * configuration为null, 是precheck的逻辑，不需要回写PK_TYPE到configuration中
     */
    private static Pair<Object, Object> checkSplitPk(Connection conn, String pkRangeSQL, int fetchSize,
                                                     ProgramInfo programInfo) {
        ResultSet rs = null;
        Pair<Object, Object> minMaxPK = null;
        try {
            try {
                rs = SingleTableSplitUtil.query(conn, pkRangeSQL, fetchSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 获得表结构
            ResultSetMetaData rsMetaData = rs.getMetaData();
            //判断 pk 类型是否有效
            if (isPKTypeValid(rsMetaData)) {
                // pk 是 string 类型
                if (isStringType(rsMetaData.getColumnType(1))) {
                    if (programInfo != null) {
                        programInfo.setPK_TYPE("pkTypeString");
                    }
                    //异步获取 resultSet 的 next()
                    while (SingleTableSplitUtil.asyncResultSetNext(rs)) {
                        //左元素是 min，右元素是 max
                        minMaxPK = new ImmutablePair<Object, Object>(
                                rs.getString(1), rs.getString(2));
                    }
                    // pk 是 long 类型
                } else if (isLongType(rsMetaData.getColumnType(1))) {
                    if (programInfo != null) {
                        programInfo.setPK_TYPE("pkTypeLong");
                    }
                    //异步获取 resultSet 的 next()
                    while (SingleTableSplitUtil.asyncResultSetNext(rs)) {
                        //左元素是 min，右元素是 max
                        minMaxPK = new ImmutablePair<Object, Object>(
                                rs.getString(1), rs.getString(2));

                        // check: string shouldn't contain '.', for oracle
                        String minMax = rs.getString(1) + rs.getString(2);
                        if (StringUtils.contains(minMax, '.')) {
                            Log.error("您配置的切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 PhotonT 不支持. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
                        }
                    }
                } else {
                    Log.error("您配置的切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 PhotonT 不支持. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
                }
            } else {
                Log.error("您配置的切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 PhotonT 不支持. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
            }
        } catch (Exception e) {
            Log.error("PhotonT 尝试切分表发生错误. 请检查您的配置并作出修改." + e.getMessage());
        } finally {
            SingleTableSplitUtil.closeDBResources(rs, null, null);
        }

        return minMaxPK;
    }

    private static boolean isPKTypeValid(ResultSetMetaData rsMetaData) {
        boolean ret = false;
        try {
            int minType = rsMetaData.getColumnType(1);
            int maxType = rsMetaData.getColumnType(2);

            boolean isNumberType = isLongType(minType);

            boolean isStringType = isStringType(minType);

            if (minType == maxType && (isNumberType || isStringType)) {
                ret = true;
            }
        } catch (Exception e) {
            Log.error("PhotonT 获取切分主键(splitPk)字段类型失败. 该错误通常是系统底层异常导致. 请联系 DBA 处理.");
        }
        return ret;
    }

    /**
     * 智能获取切分字段
     *
     * @param programInfo
     * @param table
     * @return
     */
    private static String getResultPK(ProgramInfo programInfo, String table){
        //取主键为切分字段
        if (!StringUtils.isEmpty(getPK1(table, programInfo))){
            return getPK1(table, programInfo);
        }else{
            //智能取切分字段
            String proNameAndBatchNo=programInfo.getProName()+programInfo.getBatchNO();
            JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(proNameAndBatchNo);
            String baseSql = "select * from "+ table;
            SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(baseSql);
            SqlRowSetMetaData sqlRsmd = sqlRowSet.getMetaData();
            int columnCount = sqlRsmd.getColumnCount();
            List<Map<String, String>> longTableFieldList = new ArrayList<>();
            List<Map<String, String>> stringTableFieldList = new ArrayList<>();
            String resultName = "";
            //获取所有的 Long 和 String 字段名
            for (int i = 1; i <= columnCount; i++) {
                Map<String,String> longFieldMap = new HashMap<>();
                Map<String,String> stringFieldMap = new HashMap<>();
                Boolean isLongType = isLongType(Integer.parseInt(String.valueOf(sqlRsmd.getColumnType(i))));
                Boolean isStringType = isStringType(Integer.parseInt(String.valueOf(sqlRsmd.getColumnType(i))));
                if (isLongType) {
                    longFieldMap.put("fieldName", sqlRsmd.getColumnName(i));
                    longFieldMap.put("fieldType", String.valueOf(sqlRsmd.getColumnType(i)));
                    longTableFieldList.add(longFieldMap);
                }
                if (isStringType) {
                    stringFieldMap.put("fieldName", sqlRsmd.getColumnName(i));
                    stringFieldMap.put("fieldType", String.valueOf(sqlRsmd.getColumnType(i)));
                    stringTableFieldList.add(stringFieldMap);
                }
            }
            //取 Long 类型字段名中最大数值最大者
            if (!longTableFieldList.isEmpty()) {
                Pair<Object, Object> pair = SingleTableSplitUtil.getPKRange(programInfo, longTableFieldList.get(0).get("fieldName"), table, null);
                Long max = Long.parseLong(pair.getRight().toString());
                String maxName = longTableFieldList.get(0).get("fieldName");
                for (Map<String, String> tableField : longTableFieldList) {
                    String split = tableField.get("fieldName");
                    Pair<Object, Object> minMaxPK = SingleTableSplitUtil.getPKRange(programInfo, split, table, null);
                    if (max <= Long.parseLong(minMaxPK.getRight().toString())) {
                        maxName = tableField.get("fieldName");
                        //最大数字的列
                        resultName = maxName;
                    }
                }
                return resultName;
                //取 String 类型字段名中没有汉字字符的第一个字段名
            } else if (!stringTableFieldList.isEmpty()) {
                resultName = "有汉字列";
                return resultName;
            }
            resultName = "有汉字列";
            return resultName;
        }
    }

    private static List<Range> splitChineseTable(ProgramInfo programInfo, String table, int adviceNum) {
        //获取表中 length 最长列的列名和列中 length 最小和最大值
        Map<String, Object> map = getLengthRange(programInfo, table);
        String column = map.get("column").toString();
        Float max = Float.parseFloat(map.get("max").toString());
        Float min = Float.parseFloat(map.get("min").toString());
        List<Range> lengthRange = new ArrayList<>();
        Float splitRange = (max - min) / adviceNum;
        if (adviceNum == 1) {
            Range range = new Range();
            range.setDbTableName(table);
            range.setQuery("SELECT * FROM "+ table);
            lengthRange.add(range);
            return lengthRange;
        } else {
            for (int k = 1; k <= adviceNum; k++) {
                if (k == adviceNum) {
                    String sql1 = "SELECT * FROM %s WHERE ( %s <= LENGTH(%s) AND LENGTH(%s) <= %s )";
                    String query1 = String.format(sql1, table, min + splitRange * (k - 1), column, column, min + splitRange * k);
                    Range range1 = new Range();
                    range1.setDbTableName(table);
                    range1.setQuery(query1);
                    lengthRange.add(range1);
                    String query2 = "SELECT * FROM "+ table + " WHERE LENGTH(" + column + ") is NULL";
                    Range range2 = new Range();
                    range2.setDbTableName(table);
                    range2.setQuery(query2);
                    lengthRange.add(range2);
                    break;
                }
                String sql = "SELECT * FROM %s WHERE ( %s <= LENGTH(%s) AND LENGTH(%s) < %s )";
                String query = String.format(sql, table, min + splitRange * (k - 1), column, column, min + splitRange * k);
                Range range = new Range();
                range.setDbTableName(table);
                range.setQuery(query);
                lengthRange.add(range);
            }
        }
            return lengthRange;
        }


    private static Map<String, Object> getLengthRange(ProgramInfo programInfo, String table) {
        String proNameAndBatchNo=programInfo.getProName()+programInfo.getBatchNO();
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(proNameAndBatchNo);
        //获取列名和列类型
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
                Map<String ,Object> map= list.get(0);
                if(!list.isEmpty()) {
                    if (map.get(maxKey) == null) {
                        break;
                    } else {
                        Long max = Long.parseLong(map.get(maxKey).toString());
                        Map<Long, String> maxMap = new HashMap<>();
                        maxMap.put(max, colName);
                        maxMapList.add(maxMap);
                        maxList.add(max);
                        for (Long maxNum : maxList) {
                            if (maxNum >= result) {
                                //得到 length 最大的值
                                result = maxNum;
                            }
                        }
                    }
                }
//                if(!list.isEmpty()){
//
////                    if (list.get(0).get(maxKey).equals(null)){
////                        break;
////                    }
//
//                }
            }
            for (Map<Long, String> rmap : maxMapList) {
                if (!StringUtils.isEmpty(rmap.get(result))){
                    //得到 length 最大的值对应的列名
                    resultName = rmap.get(result);
                }
            }
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
                    //得到列中length最小的值
                    minresult = minNum;
                }
            }
            Map<String, Object> map = new HashMap<>();
            map.put("column", resultName);
            map.put("min", minresult.floatValue());
            map.put("max", result.floatValue());
            return map;
    }
        return null;
}

    // warn: Types.NUMERIC is used for oracle! because oracle use NUMBER to
    // store INT, SMALLINT, INTEGER etc, and only oracle need to concern
    // Types.NUMERIC
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

    private static String genPkRangeSQL(ProgramInfo programInfo, String table, String where) {
        String splitPkName = null;
//        boolean hasSplitPk = StringUtils.isNotBlank(programInfo.getSplitPk());
//        splitPkName = hasSplitPk ? programInfo.getSplitPk().trim() : SingleTableSplitUtil.getPK(table, programInfo);
        if (StringUtils.isNotBlank(programInfo.getSplitPk())) {
            splitPkName = programInfo.getSplitPk();
        } else {
            splitPkName = getResultPK(programInfo, table);
        }
        //去掉SPLIT_PK前面和后面的空格
        //去掉TABLE前面和后面的空格
        String table1 = table.trim();
        //取配置中where 没有where就为null
//        String where = programInfo.getString(Key.WHERE, null);
        return genPKSql(splitPkName, table1, where);
    }

    public static String genPKSql(String splitPK, String table, String where) {
        //构建sql语句 ： 从 table 中取 给定字段的最小值 最大值
        String minMaxTemplate = "SELECT MIN(%s),MAX(%s) FROM %s";
        // 字段构建的范围sql pkRangeSQL
        String pkRangeSQL = String.format(minMaxTemplate, splitPK, splitPK,
                table);
        // 提供了 where 的情况
        if (StringUtils.isNotBlank(where)) {
            // 这要是没有 splitPK 字段的数据就不会被切割到了
            pkRangeSQL = String.format("%s WHERE (%s AND %s IS NOT NULL)",
                    pkRangeSQL, where, splitPK);
        }
        return pkRangeSQL;
    }

    /**
     * 获取表主键字段名
     *
     * @param table
     * @param programInfo
     * @return
     */
    //TODO
    public static String getPK(String table, ProgramInfo programInfo) {
        String proNameAndBatchNo=programInfo.getProName()+programInfo.getBatchNO();
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(proNameAndBatchNo);
        String PKName = null;
        String baseSql = "select * from "+ table;
        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet(baseSql);
        SqlRowSetMetaData sqlRsmd = sqlRowSet.getMetaData();
        String fieldName = String.valueOf(sqlRsmd.getColumnName(1));
        String fieldType = String.valueOf(sqlRsmd.getColumnType(1));
        //获取所有的 Long 和 String 字段名
        Boolean isLongType = isLongType(Integer.parseInt(fieldType));
        Boolean isStringType = isStringType(Integer.parseInt(fieldType));
        //取 Long 类型字段名中最大数值最大者
        if (isLongType) {
            PKName = fieldName;
            return PKName;
        } else if (isStringType) {
            PKName = "有汉字列";
            return PKName;
        }
        return null;

    }

    public static String getPK1(String table, ProgramInfo programInfo) {
        //TODO get
        String procNameAndBatchNoAndSourceDsName = programInfo.getProName() + programInfo.getBatchNO()+programInfo.getSourceDsName();
        Connection conn = MySqlConnection.getConnection(procNameAndBatchNoAndSourceDsName);
        String PKName = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            String[] tables = StringUtils.split(table,".");
            ResultSet rs = dmd.getPrimaryKeys(null, "%", tables[1]);
            rs.next();
            PKName = rs.getString("column_name");
            rs.close();
            return PKName;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }

    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn Database connection .
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize)
            throws SQLException {
        // 默认3600 s 的query Timeout
        return query(conn, sql, fetchSize, 172800);
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn         Database connection .
     * @param sql          sql statement to be executed
     * @param fetchSize
     * @param queryTimeout unit:second
     * @return
     * @throws SQLException
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize, int queryTimeout)
            throws SQLException {
        // make sure autocommit is off
        conn.setAutoCommit(false);
        // ？
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(fetchSize);
        stmt.setQueryTimeout(queryTimeout);
        return query(stmt, sql);
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param stmt {@link Statement}
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Statement stmt, String sql)
            throws SQLException {
        return stmt.executeQuery(sql);
    }

    /**
     * 异步获取resultSet的next(),注意，千万不能应用在数据的读取中。只能用在meta的获取
     * @param resultSet
     * @return
     */
    public static boolean asyncResultSetNext(final ResultSet resultSet) {
        return asyncResultSetNext(resultSet, 3600);
    }

    public static boolean asyncResultSetNext(final ResultSet resultSet, int timeout) {
        Future<Boolean> future = rsExecutors.get().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return resultSet.next();
            }
        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.error("异步获取ResultSet失败" + e.getMessage());
        }
        return Boolean.FALSE;
    }

    private static final ThreadLocal<ExecutorService> rsExecutors = new ThreadLocal<ExecutorService>() {
        @Override
        protected ExecutorService initialValue() {
            return Executors.newFixedThreadPool(1, new ThreadFactoryBuilder()
                    .setNameFormat("rsExecutors-%d")
                    .setDaemon(true)
                    .build());
        }
    };

    public static void closeDBResources(ResultSet rs, Statement stmt,
                                        Connection conn) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException unused) {
            }
        }

        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException unused) {
            }
        }

        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException unused) {
            }
        }
    }

}