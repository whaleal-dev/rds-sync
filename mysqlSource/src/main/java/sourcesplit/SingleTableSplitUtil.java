package sourcesplit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import common.dataclass.Range;
import conf.Configuration;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import util.Log;

import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


public class SingleTableSplitUtil {

    public static List<Range> splitSingleTable(Configuration configuration, String table, int adviceNum) {

        List<Range> pluginParams = new ArrayList<Range>();
        List<String> rangeList = null;
        //从配置中取分片字段 splitPk
        //TODO 取主键
        String splitPkName = null;
        boolean hasSplitPk = StringUtils.isNotBlank(splitPkName);
        splitPkName = hasSplitPk ? configuration.getSplitPk() : SingleTableSplitUtil.getPK(table, configuration);
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
            Log.error("根据切分主键切分表失败. PhotonT 仅支持切分主键为一个,并且类型为整数或者字符串类型. 请尝试使用其他的切分主键或者联系 DBA 进行处理.");
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
    private static Pair<Object, Object> getPkRange(Configuration configuration, String table, String where) {
        //字段构建的范围 sql pkRangeSQL
        String pkRangeSQL = genPKRangeSQL(configuration, table, where);
        //取配置中的 fetchSize
        int fetchSize = configuration.getFetchSize();
        //获取连接
        Connection conn = MySqlConnection.createConnection(configuration.getSourceDsName(),
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
                    if (configuration != null) {
                        configuration.setPK_TYPE("pkTypeString");
                    }
                    //异步获取 resultSet 的 next()
                    while (SingleTableSplitUtil.asyncResultSetNext(rs)) {
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
        String splitPkName = null;
        boolean hasSplitPk = StringUtils.isNotBlank(splitPkName);
        splitPkName = hasSplitPk ? configuration.getSplitPk().trim() : SingleTableSplitUtil.getPK(table, configuration);
        //去掉SPLIT_PK前面和后面的空格
        //去掉TABLE前面和后面的空格
        String table1 = table.trim();
        //取配置中where 没有where就为null
//        String where = configuration.getString(Key.WHERE, null);
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
     * @param configuration
     * @return
     */
    public static String getPK(String table, Configuration configuration) {
        Connection conn = MySqlConnection.createConnection(configuration.getSourceDsName(),
                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
        String PKName = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            String[] tables = StringUtils.split(table,".");
            ResultSet rs = dmd.getPrimaryKeys(null, "%", tables[1]);
            rs.next();
            PKName = rs.getString("column_name");
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return PKName;
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