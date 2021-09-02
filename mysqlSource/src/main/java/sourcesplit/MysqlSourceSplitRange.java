package sourcesplit;

import common.dataclass.Range;
import common.photonV.entity.ProgramInfo;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlSourceSplitRange {

    public static List<Range> doSplit(ProgramInfo programInfo) throws SQLException {

        //初始化单表应该切分的份数
        int adviceNumber = programInfo.getAdviceNumber();
        int tableNumber = MysqlSourceSplitRange.getTableNumber(programInfo);
        int eachTableShouldSplittedNumber = -1;
        // adviceNumber这里是并发task数量
        // eachTableShouldSplittedNumber是单表应该切分的份数, 向上取整可能和adviceNumber没有比例关系了已经
        eachTableShouldSplittedNumber = calculateEachTableShouldSplittedNumber(
                adviceNumber, tableNumber);
        List<Range> splittedRanges = new ArrayList<Range>();
        List<String> tables = getDbTables(programInfo);
        //单表
        //TODO
        /*if (tables.size() == 1) {
            Integer splitFactor = programInfo.getSplitFactor();
            eachTableShouldSplittedNumber = eachTableShouldSplittedNumber * splitFactor;
        }*/
        for (String table : tables) {
            String tempTable = table;
            List<Range> splittedSlices = SingleTableSplitUtil
                    .splitSingleTable(programInfo, tempTable, eachTableShouldSplittedNumber);
            splittedRanges.addAll(splittedSlices);
        }

        return splittedRanges;
    }

    private static int calculateEachTableShouldSplittedNumber(int adviceNumber, int tableNumber) {
        double tempNum = 1.0 * adviceNumber / tableNumber;
        //math.ceil(x)返回大于等于参数x的最小整数,即对浮点数向上取整
        return (int) Math.ceil(tempNum);
    }


    public static List<String> getDbTables(ProgramInfo programInfo) throws SQLException {
        //获取连接
        Connection conn = MySqlConnection.createConnection(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));
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
            String dbTable = dbName + "." + tableName;
            if (dbTable.matches(programInfo.getDbTableWhite())) {
                dbTables.add(dbTable);
            }
        }
        rs.close();
        return dbTables;
    }

    public static int getTableNumber(ProgramInfo programInfo) throws SQLException {
        return MysqlSourceSplitRange.getDbTables(programInfo).size();
    }

}
