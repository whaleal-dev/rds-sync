package conf;

import common.dataclass.Range;
import constant.CommonConstant;
import constant.Key;
import constant.utils.Constant;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.Log;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderSplitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ReaderSplitUtil.class);

    private static Pattern mysqlPattern = Pattern.compile("jdbc:mysql://(.+):\\d+/.+");
    private static Pattern oraclePattern = Pattern.compile("jdbc:oracle:thin:@(.+):\\d+:.+");

    //RangeList
    public static List<Range> doSplit(Configuration configuration, int adviceNumber, int tableNumber) throws SQLException {
        //TODO 判断表模式
//        boolean isTableMode = configuration.getBool(Constant.IS_TABLE_MODE).booleanValue();
        //初始化单表应该切分的份数
        int eachTableShouldSplittedNumber = -1;
        //TODO
//        if (isTableMode) {
            // adviceNumber这里是channel数量大小, 即datax并发task数量
            // eachTableShouldSplittedNumber是单表应该切分的份数, 向上取整可能和adviceNumber没有比例关系了已经
            // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
            eachTableShouldSplittedNumber = calculateEachTableShouldSplittedNumber(
                    adviceNumber, tableNumber);
//        }

//        String column = configuration.getString(Key.COLUMN);
//        String where = configuration.getString(Key.WHERE, null);

        //获取连接集合
//        List<Object> conns = configuration.getList(Constant.CONN_MARK, Object.class);
        //创建一个存放 分好片的配置的 list 最后返回
        List<Range> splittedRanges = new ArrayList<Range>();

        //配置中可能有好几个connection  不要了就一个
//        for (int i = 0, len = conns.size(); i < len; i++) {
        //
//            Configuration sliceConfig = configuration.clone();
            //
//        Configuration connConf = Configuration.from(conns.toString());
//            Configuration connConf = Configuration.from(conns.get(i).toString());
            //

//            String jdbcUrl = connConf.getString(Key.JDBC_URL);
            //切分的不需要了 总配置取
//            sliceConfig.set(Key.JDBC_URL, jdbcUrl);
            // 抽取 jdbcUrl 中的 ip/port 进行资源使用的打标，以提供给 core 做有意义的 shuffle 洗牌操作
            //可以不要了
//            sliceConfig.set(CommonConstant.LOAD_BALANCE_RESOURCE_MARK,
                    //目前只实现了从 mysql/oracle 中识别出ip 信息.未识别到则返回 null.
//                    parseIpFromJdbcUrl(jdbcUrl));
            //连接去除
//            sliceConfig.remove(Constant.CONN_MARK);
            //tempSlice临时切片
            Configuration tempSlice;
            // TODO 说明是配置的 table 方式
//            if (isTableMode) {
                // 已在之前进行了扩展和`处理，可以直接使用
                //取配置里面的表 table
                List<String> tables = getDbTables(configuration.getSourceDsName(), configuration.getDbTableWhite());

                Validate.isTrue(null != tables && !tables.isEmpty(), "您读取数据库表配置错误.");
                //TODO
                String splitPk = configuration.getSplitPk();
                //最终切分份数不一定等于 eachTableShouldSplittedNumber
                //单表
                //TODO 所有表 getAllTables
                if (tables.size() == 1) {
                    //原来:如果是单表的，主键切分num=num*2+1
                    // splitPk is null这类的情况的数据量本身就比真实数据量少很多, 和channel大小比率关系时，不建议考虑
                    //eachTableShouldSplittedNumber = eachTableShouldSplittedNumber * 2 + 1;// 不应该加1导致长尾

                    //考虑其他比率数字?(splitPk is null, 忽略此长尾)
                    //eachTableShouldSplittedNumber = eachTableShouldSplittedNumber * 5;

                    //为避免导入hive小文件 默认基数为5，可以通过 splitFactor 配置基数
                    // 最终task数为(channel/tableNum)向上取整*splitFactor
                    // 配置基数 splitFactor默认为5
                    // 没配置Key.SPLIT_FACTOR的话，就是默认为5
                    Integer splitFactor = configuration.getSplitFactor();
                    // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
                    eachTableShouldSplittedNumber = eachTableShouldSplittedNumber * splitFactor;
                }
                // 尝试对每个表，切分为eachTableShouldSplittedNumber 份
                for (String table : tables) {
                    //拷贝配置给临时分片
                    //配置临时分片的table名
                    String tempTable = table;

//                    tempSlice.set(Key.TABLE, table);
                    //splittedSlices 已经分片的
                    //tempSlice 临时分片配置
                    // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
                    List<Range> splittedSlices = SingleTableSplitUtil
                            .splitSingleTable(configuration, tempTable, eachTableShouldSplittedNumber);
//                    List<Configuration> splittedSlices = SingleTableSplitUtil
//                            .splitSingleTable(tempSlice, eachTableShouldSplittedNumber);
                    //将指定 splittedSlices 中的所有元素添加到集合
                    splittedRanges.addAll(splittedSlices);
                }
//            }
//            else {
//                //TODO 说明是配置的 querySql 方式
//                List<String> sqls = connConf.getList(Key.QUERY_SQL, String.class);
//                // TODO 是否check 配置为多条语句？？
//                for (String querySql : sqls) {
//                    tempSlice = sliceConfig.clone();
//                    tempSlice.set(Key.QUERY_SQL, querySql);
//                    splittedConfigs.add(tempSlice);
//                }
//            }
//        }

        return splittedRanges;
    }

    private static int calculateEachTableShouldSplittedNumber(int adviceNumber, int tableNumber) {

        double tempNum = 1.0 * adviceNumber / tableNumber;
        //math.ceil(x)返回大于等于参数x的最小整数,即对浮点数向上取整
        return (int) Math.ceil(tempNum);
    }

    public static String parseIpFromJdbcUrl(String jdbcUrl) {

        Matcher mysql = mysqlPattern.matcher(jdbcUrl);
        if (mysql.matches()) {
            return mysql.group(1);
        }
        Matcher oracle = oraclePattern.matcher(jdbcUrl);
        if (oracle.matches()) {
            return oracle.group(1);
        }
        return null;
    }

    public static List<String> getDbTables(String sourceName, String dbTableWhite) throws SQLException {
//        List<JSONObject> connConfList = conf.getList(Key.CONNECTION, JSONObject.class);
//        Connection conn = DBUtil.getConnection(conf);
        //获取连接
        Connection conn = MySqlConnection.getConnection(sourceName);
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = metaData.getTables(null, null, "%", types);
        List<String> dbTables = new ArrayList<>();
        while(rs.next()){
            //1 TABLE_CAT String => table catalog (may be null)
            //2 TABLE_SCHEM String => table schema (may be null)
            //3 TABLE_NAME String => table name
            //获取数据库表名
            String tableName = rs.getString(3);
            //获取数据库名
            String dbName = rs.getString(1);
            //读取配置中的table
//            String tableConf = StringUtils.strip(connConfList.get(0).getString(Key.TABLE), "[]").replaceAll("\"", "");
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.add(dbTable);
            }
        }
        rs.close();
        return dbTables;
    }

    public static int getTableNumber(String sourceName, String dbTableWhite) throws SQLException {
//        List<JSONObject> connConfList = conf.getList(Key.CONNECTION, JSONObject.class);
//        Connection conn = DBUtil.getConnection(conf);
        //获取连接
        Connection conn = MySqlConnection.getConnection(sourceName);
        DatabaseMetaData metaData = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = metaData.getTables(null, null, "%", types);
        List<String> dbTables = new ArrayList<>();
        while(rs.next()){
            //1 TABLE_CAT String => table catalog (may be null)
            //2 TABLE_SCHEM String => table schema (may be null)
            //3 TABLE_NAME String => table name
            //获取数据库表名
            String tableName = rs.getString(3);
            //获取数据库名
            String dbName = rs.getString(1);
            //读取配置中的table
//            String tableConf = StringUtils.strip(connConfList.get(0).getString(Key.TABLE), "[]").replaceAll("\"", "");
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(dbTableWhite)) {
                dbTables.add(dbTable);
            }
        }
        rs.close();
        int tableNumber = dbTables.size();
        return tableNumber;
    }

}
