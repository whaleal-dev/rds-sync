package sourcesplit;


import common.dataclass.Range;
import dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;
import util.split.RangeSplitUtil;

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
        String[] split = dbTableName.split("\\.", 2);
        String dbName = split[0];
        String tableName = split[1];
        String sql = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema=? AND table_name =? order by ordinal_position ";
        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(sql, dbName, tableName);
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
            if (maxDiffTemp == 0 && (range.getColumnName() == null || range.getColumnName().equals(""))) {
                Range rangeOfNull = new Range();
                rangeOfNull.setColumnName(range.getColumnName());
                rangeOfNull.setDbTableName(dbTableName);
                rangeOfNull.setQuery("( 1=1 )");
                rangeList.add(rangeOfNull);
            } else {
                rangeList = RangeSplitUtil.getRangeListByLongType((Integer) range.getMinId(), (Integer) range.getMaxId(), 10, range.getColumnName());
            }
        } else {
            Range rangeOfNull = new Range();
            rangeOfNull.setDbTableName(dbTableName);
            rangeOfNull.setQuery("(1=1)");
            rangeList.add(rangeOfNull);
        }
        rangeList.forEach(range -> System.out.println(range.getQuery()));
        return rangeList;
    }

    public Map<String, Object> getMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0L;
        int min = 0;
        int max = 0;
        String sql = "select min(" + intColumnName + ")  min, max(" + intColumnName + ")  max from " + dbTableName + "";
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                min = (Integer) mapList.get(0).get("min");
                max = (Integer) mapList.get(0).get("max");
            }
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

}
