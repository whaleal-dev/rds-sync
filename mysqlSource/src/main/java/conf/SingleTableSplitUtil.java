package conf;

import common.dataclass.Range;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Log;

import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class SingleTableSplitUtil {
//    private static final Logger LOG = LoggerFactory
//            .getLogger(SingleTableSplitUtil.class);

//    private SingleTableSplitUtil() {
//    }

    //对单表进行分表
    // tempSlice 临时分片配置 = configuration
    // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
    // adviceNum = eachTableShouldSplittedNumber
    public static List<Range> splitSingleTable(Configuration configuration, String table, int adviceNum) {
        //插件参数集合
        List<Range> pluginParams = new ArrayList<Range>();
        //范围集合
        List<String> rangeList = null;
        //从配置中取分片字段 splitPk
        //TODO 智能取分片字段
        String splitPkName = configuration.getSplitPk();
        //从配置中取列 column
        //默认 *
        String column = "*";
        //取table
        //
        //取where 取不到就为null
        String where = null;
        //配置中有无where
        boolean hasWhere = StringUtils.isNotBlank(where);

        //String splitMode = configuration.getString(Key.SPLIT_MODE, "");
        //if (Constant.SPLIT_MODE_RANDOMSAMPLE.equals(splitMode) && DATABASE_TYPE == DataBaseType.Oracle) {
        // Pair Java中的配对
        // minMaxPK 最小到最大字段

        Pair<Object, Object> minMaxPK = getPkRange(configuration, table, where);
        if (null == minMaxPK) {
            Log.error("根据切分主键切分表失败. DataX 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
        }
        Range range = new Range();

        range.setQuery(buildQuerySql(column, table, where));
//        configuration.set(Key.QUERY_SQL, buildQuerySql(column, table, where));

        // 切分后获取到的 start/end 有 Null 的情况
        if (null == minMaxPK.getLeft() || null == minMaxPK.getRight()) {
            pluginParams.add(range);
            return pluginParams;
        }

        boolean isStringType = "pkTypeString".equals(configuration
                .getPK_TYPE());
        boolean isLongType = "pkTypeLong".equals(configuration
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
            Log.error("您配置的切分主键(splitPk) 类型 DataX 不支持. DataX 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
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
            //pluginParams.add(configuration); // this is wrong for new & old split
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
        tempQuerySql =

                buildQuerySql(column, table, where)
                        + (hasWhere ? " and " : " where ")
                        + String.format(" %s IS NULL", splitPkName);

        allQuerySql.add(tempQuerySql);

//        LOG.info("After split(), allQuerySql=[\n{}\n].",
//                StringUtils.join(allQuerySql, "\n"));
        range = new Range();
        range.setDbTableName(table);
        range.setQuery(tempQuerySql);
        pluginParams.add(range);

        return pluginParams;
    }

    public static String buildQuerySql(String column, String table,
                                       String where) {
        String querySql;

        if (StringUtils.isBlank(where)) {
            // "select %s from %s ";
            querySql = String.format("select %s from %s ",
                    column, table);
        } else {
            // "select %s from %s where (%s)";
            querySql = String.format("select %s from %s where (%s)", column,
                    table, where);
        }

        return querySql;
    }

    @SuppressWarnings("resource")
    private static Pair<Object, Object> getPkRange(Configuration configuration, String table, String where) {
        //字段构建的范围 sql pkRangeSQL
        String pkRangeSQL = genPKRangeSQL(configuration, table, where);
        //取配置中的 fetchSize
        int fetchSize = configuration.getFetchSize();
        //取配置中的 jdbcURL
//        String jdbcURL = configuration.getString(Key.JDBC_URL);
        //取配置中的 username
//        String username = configuration.getString(Key.USERNAME);
        //取配置中的 password
//        String password = configuration.getString(Key.PASSWORD);
        //取配置中的 table
        //获取连接
        Connection conn = MySqlConnection.getConnection(configuration.getSourceDsName(),
                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
        //字段构建的范围 sql pkRangeSQL
        Pair<Object, Object> minMaxPK = checkSplitPk(conn, pkRangeSQL, fetchSize, configuration);

        return minMaxPK;
    }

    /**
     * 检测splitPk的配置是否正确。
     * configuration为null, 是precheck的逻辑，不需要回写PK_TYPE到configuration中
     */
    private static Pair<Object, Object> checkSplitPk(Connection conn, String pkRangeSQL, int fetchSize,
                                                     Configuration configuration) {
        ResultSet rs = null;
        Pair<Object, Object> minMaxPK = null;
        try {
            try {
                rs = MySqlConnection.query(conn, pkRangeSQL, fetchSize);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 获得表结构
            ResultSetMetaData rsMetaData = rs.getMetaData();
            //判断 pk 类型是否有效
            if (isPKTypeValid(rsMetaData)) {
                // pk 是 string 类型
                if (isStringType(rsMetaData.getColumnType(1))) {
                    if (configuration != null) {
                        configuration.setPK_TYPE("pkTypeString");
                    }
                    //异步获取 resultSet 的 next()
                    while (MySqlConnection.asyncResultSetNext(rs)) {
                        //左元素是 min，右元素是 max
                        minMaxPK = new ImmutablePair<Object, Object>(
                                rs.getString(1), rs.getString(2));
                    }
                    // pk 是 long 类型
                } else if (isLongType(rsMetaData.getColumnType(1))) {
                    if (configuration != null) {
                        configuration.setPK_TYPE("pkTypeLong");
                    }
                    //异步获取 resultSet 的 next()
                    while (MySqlConnection.asyncResultSetNext(rs)) {
                        //左元素是 min，右元素是 max
                        minMaxPK = new ImmutablePair<Object, Object>(
                                rs.getString(1), rs.getString(2));

                        // check: string shouldn't contain '.', for oracle
                        String minMax = rs.getString(1) + rs.getString(2);
                        if (StringUtils.contains(minMax, '.')) {
                            Log.error("您配置的DataX切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 DataX 不支持. DataX 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
                        }
                    }
                } else {
                    Log.error("您配置的DataX切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 DataX 不支持. DataX 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
                }
            } else {
                Log.error("您配置的DataX切分主键(splitPk)有误. 因为您配置的切分主键(splitPk) 类型 DataX 不支持. DataX 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
            }
        } catch (Exception e) {
            Log.error("DataX尝试切分表发生错误. 请检查您的配置并作出修改." + e.getMessage());
        } finally {
           MySqlConnection.closeDBResources(rs, null, null);
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
            Log.error("DataX获取切分主键(splitPk)字段类型失败. 该错误通常是系统底层异常导致. 请联系旺旺:askdatax或者DBA处理.");
        }
        return ret;
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

    private static String genPKRangeSQL(Configuration configuration, String table, String where) {
        //去掉SPLIT_PK前面和后面的空格
        String splitPK = configuration.getSplitPk().trim();
        //去掉TABLE前面和后面的空格
        String table1 = table.trim();
        //取配置中where 没有where就为null
//        String where = configuration.getString(Key.WHERE, null);
        return genPKSql(splitPK, table1, where);
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


}