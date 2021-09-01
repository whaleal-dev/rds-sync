package sourcesplit;


import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import common.dataclass.Range;
import common.dbtype.MongoDbTypeNumber;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.pgserver.PgServerConnection;
import org.bson.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import java.util.*;

/**
 * @author: lhp
 * @time: 2021/7/16 3:04 下午
 * @desc:
 */
public class PgSourceSplitRange {

    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public PgSourceSplitRange(String sourceDsName) {
        this.sourceDsName = sourceDsName;
        this.jdbcTemplate = PgServerConnection.getJdbcTemplate(sourceDsName);
    }


    public List<Range> getRangeList(String dbTableName) {
        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(
                "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_catalog='test' AND table_name ='student' order by ordinal_position ");
        Set<String> intColumnSet = new HashSet<>();
        for (Map<String, Object> columnMap : tableMeteColumn) {
            if ("INTEGER".equalsIgnoreCase(columnMap.get("data_type").toString())) {
                intColumnSet.add(columnMap.get("column_name").toString());
            }
        }
        List<Range> rangeList = new ArrayList<>();
        if (intColumnSet.size() > 0) {
            Range range = new Range();
            range.setDbTableName(dbTableName);
            long maxDiffTemp = 0;
            for (String intColumnName : intColumnSet) {
                Map<String, Object> infoMap = getMaxDifference(intColumnName, dbTableName);
                Long difference = (Long) infoMap.get("difference");
                int min = (Integer) infoMap.get("min");
                int max = (Integer) infoMap.get("max");
                if (difference > maxDiffTemp) {
                    range.setColumnName(intColumnName);
                    range.setMaxId(max);
                    range.setMinId(min);
                    maxDiffTemp = difference;
                }
            }

            rangeList = getRangeList(range, 3);
            System.out.println(range);
            Range rangeOfNull = new Range();
            rangeOfNull.setColumnName(range.getColumnName());
            rangeOfNull.setDbTableName(dbTableName);
            rangeOfNull.setQuery("(" + range.getColumnName() + " is null)");
            rangeList.add(rangeOfNull);

        } else {
            Range rangeOfNull = new Range();
            rangeOfNull.setDbTableName(dbTableName);
            rangeOfNull.setQuery("(1=1)");
            rangeList.add(rangeOfNull);
        }

        return rangeList;
    }

    public Map<String, Object> getMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0;
        int min = 0;
        int max = 0;
        try {
            min = jdbcTemplate.queryForObject("select min(" + intColumnName + ") from " + dbTableName + "", Integer.class);
            max = jdbcTemplate.queryForObject("select max(" + intColumnName + ") from " + dbTableName + "", Integer.class);
        } catch (Exception e) {
            Log.error(e.getMessage());
            min = 0;
            max = 0;
        }
        difference = (max - min);
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("min", min);
        infoMap.put("max", max);
        infoMap.put("difference", difference);
        return infoMap;
    }


    public static List<Range> getRangeList(Range range, int splitNum) {
        int min = (Integer) range.getMinId();
        int max = (Integer) range.getMaxId();
        long rangeNum = (long) ((max - min) / splitNum);
        String columnName = range.getColumnName();
        int minTemp = min;
        List<Range> rangeList = new ArrayList<>();
        if (rangeNum == 0 || (max - min == 0)) {
            return rangeList;
        }
        do {
            Range rangeTemp = new Range();
            String query = "(  " + columnName + ">=";
            rangeTemp.setMinId(minTemp);
            query += minTemp;
            minTemp += rangeNum;
            rangeTemp.setMaxId(minTemp);
            if (minTemp > max) {
                query += " and " + columnName + "<=" + minTemp + ")";
                rangeTemp.setQuery(query);
                rangeTemp.setMax(true);
                rangeList.add(rangeTemp);
                break;
            }
            query += " and " + columnName + "<" + minTemp + ")";
            rangeTemp.setQuery(query);
            rangeList.add(rangeTemp);

        } while (minTemp < max);
        return rangeList;
    }
}
