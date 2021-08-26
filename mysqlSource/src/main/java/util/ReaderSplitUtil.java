package util;

import conf.Configuration;
import constant.CommonConstant;
import constant.Key;
import constant.utils.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ReaderSplitUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ReaderSplitUtil.class);

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
                    DataBaseType.parseIpFromJdbcUrl(jdbcUrl));
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
                //判断是否需要分表
                boolean needSplitTable = eachTableShouldSplittedNumber > 1 && StringUtils.isNotBlank(splitPk);
                if (needSplitTable) {
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
                    //不需要分表
                    for (String table : tables) {
                        tempSlice = sliceConfig.clone();
                        tempSlice.set(Key.TABLE, table);
                        String queryColumn = HintUtil.buildQueryColumn(jdbcUrl, table, column);
                        tempSlice.set(Key.QUERY_SQL, SingleTableSplitUtil.buildQuerySql(queryColumn, table, where));
                        splittedConfigs.add(tempSlice);
                    }
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

    public static Configuration doPreCheckSplit(Configuration originalSliceConfig) {
        Configuration queryConfig = originalSliceConfig.clone();
        boolean isTableMode = originalSliceConfig.getBool(Constant.IS_TABLE_MODE).booleanValue();

        String splitPK = originalSliceConfig.getString(Key.SPLIT_PK);
        String column = originalSliceConfig.getString(Key.COLUMN);
        String where = originalSliceConfig.getString(Key.WHERE, null);

        List<Object> conns = queryConfig.getList(Constant.CONN_MARK, Object.class);

        for (int i = 0, len = conns.size(); i < len; i++){
            Configuration connConf = Configuration.from(conns.get(i).toString());
            List<String> querys = new ArrayList<String>();
            List<String> splitPkQuerys = new ArrayList<String>();
            String connPath = String.format("connection[%d]",i);
            // 说明是配置的 table 方式
            if (isTableMode) {
                // 已在之前进行了扩展和`处理，可以直接使用
                List<String> tables = connConf.getList(Key.TABLE, String.class);
                Validate.isTrue(null != tables && !tables.isEmpty(), "您读取数据库表配置错误.");
                for (String table : tables) {
                    querys.add(SingleTableSplitUtil.buildQuerySql(column,table,where));
                    if (splitPK != null && !splitPK.isEmpty()){
                        splitPkQuerys.add(SingleTableSplitUtil.genPKSql(splitPK.trim(),table,where));
                    }
                }
                if (!splitPkQuerys.isEmpty()){
                    connConf.set(Key.SPLIT_PK_SQL,splitPkQuerys);
                }
                connConf.set(Key.QUERY_SQL,querys);
                queryConfig.set(connPath,connConf);
            } else {
                // 说明是配置的 querySql 方式
                List<String> sqls = connConf.getList(Key.QUERY_SQL,
                        String.class);
                for (String querySql : sqls) {
                    querys.add(querySql);
                }
                connConf.set(Key.QUERY_SQL,querys);
                queryConfig.set(connPath,connConf);
            }
        }
        return queryConfig;
    }

    private static int calculateEachTableShouldSplittedNumber(int adviceNumber,
                                                              int tableNumber) {
        double tempNum = 1.0 * adviceNumber / tableNumber;
        //math.ceil(x)返回大于等于参数x的最小整数,即对浮点数向上取整
        return (int) Math.ceil(tempNum);
    }

}
