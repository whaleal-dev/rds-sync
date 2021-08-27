package conf;

import constant.CommonConstant;
import constant.Key;
import constant.utils.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderSplitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ReaderSplitUtil.class);

    private static Pattern mysqlPattern = Pattern.compile("jdbc:mysql://(.+):\\d+/.+");
    private static Pattern oraclePattern = Pattern.compile("jdbc:oracle:thin:@(.+):\\d+:.+");

    public static List<Configuration> doSplit(Configuration originalSliceConfig, int adviceNumber) {
        boolean isTableMode = originalSliceConfig.getBool(Constant.IS_TABLE_MODE).booleanValue();
        //初始化单表应该切分的份数
        int eachTableShouldSplittedNumber = -1;
        if (isTableMode) {
            // adviceNumber这里是channel数量大小, 即datax并发task数量
            // eachTableShouldSplittedNumber是单表应该切分的份数, 向上取整可能和adviceNumber没有比例关系了已经
            // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
            eachTableShouldSplittedNumber = calculateEachTableShouldSplittedNumber(
                    adviceNumber, originalSliceConfig.getInt(Constant.TABLE_NUMBER_MARK));
        }

        String column = originalSliceConfig.getString(Key.COLUMN);
        String where = originalSliceConfig.getString(Key.WHERE, null);

        List<Object> conns = originalSliceConfig.getList(Constant.CONN_MARK, Object.class);
        //创建一个存放 分好片的配置的 list 最后返回
        List<Configuration> splittedConfigs = new ArrayList<Configuration>();

        //配置中可能有好几个connection
        for (int i = 0, len = conns.size(); i < len; i++) {
            Configuration sliceConfig = originalSliceConfig.clone();
            Configuration connConf = Configuration.from(conns.get(i).toString());
            String jdbcUrl = connConf.getString(Key.JDBC_URL);
            sliceConfig.set(Key.JDBC_URL, jdbcUrl);
            // 抽取 jdbcUrl 中的 ip/port 进行资源使用的打标，以提供给 core 做有意义的 shuffle 洗牌操作
            sliceConfig.set(CommonConstant.LOAD_BALANCE_RESOURCE_MARK,
                    //目前只实现了从 mysql/oracle 中识别出ip 信息.未识别到则返回 null.
                    parseIpFromJdbcUrl(jdbcUrl));
            //连接去除
            sliceConfig.remove(Constant.CONN_MARK);
            //tempSlice临时切片
            Configuration tempSlice;
            // 说明是配置的 table 方式
            if (isTableMode) {
                // 已在之前进行了扩展和`处理，可以直接使用
                //取配置里面的表 table
                List<String> tables = connConf.getList(Key.TABLE, String.class);
                Validate.isTrue(null != tables && !tables.isEmpty(), "您读取数据库表配置错误.");
                String splitPk = originalSliceConfig.getString(Key.SPLIT_PK, null);
                //最终切分份数不一定等于 eachTableShouldSplittedNumber
                //单表
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
                    Integer splitFactor = originalSliceConfig.getInt(Key.SPLIT_FACTOR, Constant.SPLIT_FACTOR);
                    // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
                    eachTableShouldSplittedNumber = eachTableShouldSplittedNumber * splitFactor;
                }
                // 尝试对每个表，切分为eachTableShouldSplittedNumber 份
                for (String table : tables) {
                    //拷贝配置给临时分片
                    tempSlice = sliceConfig.clone();
                    //配置临时分片的table名
                    tempSlice.set(Key.TABLE, table);
                    //splittedSlices 已经分片的
                    //tempSlice 临时分片配置
                    // tempNum = 1.0 * adviceNumber / tableNumber ， tempNum再向上取整 = eachTableShouldSplittedNumber
                    List<Configuration> splittedSlices = SingleTableSplitUtil
                            .splitSingleTable(tempSlice, eachTableShouldSplittedNumber);
                    //将指定 splittedSlices 中的所有元素添加到集合
                    splittedConfigs.addAll(splittedSlices);
                }
            } else {
                // 说明是配置的 querySql 方式
                List<String> sqls = connConf.getList(Key.QUERY_SQL, String.class);
                // TODO 是否check 配置为多条语句？？
                for (String querySql : sqls) {
                    tempSlice = sliceConfig.clone();
                    tempSlice.set(Key.QUERY_SQL, querySql);
                    splittedConfigs.add(tempSlice);
                }
            }

        }

        return splittedConfigs;
    }

    private static int calculateEachTableShouldSplittedNumber(int adviceNumber,
                                                              int tableNumber) {
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

}
