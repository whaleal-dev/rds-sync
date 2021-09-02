package com.whaleal.photon.source.oracle.split;

import common.dataclass.Range;
import dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 甲骨文源分离范围
 *
 * @author cs
 * @date 2021/09/01
 */
public class OracleSourceSplitRange {
    /**
     * jdbcTemplate
     */
    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public OracleSourceSplitRange(String sourceDsName) {
        this.sourceDsName = sourceDsName;
        this.jdbcTemplate = OracleConnection.getJdbcTemplate(sourceDsName);
    }

    public List<Range> getRangeList(String dbTableName) {

        String tableName = dbTableName.split("\\.", 2)[1];
        //根据sql语句来查询得到最终的结果
        String sql = "select a.constraint_name,  a.column_name from user_cons_columns a, user_constraints b where a.constraint_name = b.constraint_name  and b.constraint_type = 'P' and a.table_name = '" + tableName + "'";
        Map<String, Object> tableMeteColumn = jdbcTemplate.queryForMap(sql);
        List<Range> rangeList = new ArrayList<>();
        //根据什么键进行切分需要进行一个判断，如果有的话就去找，如果没有的话就算了
        if (!StringUtils.isEmpty(tableMeteColumn)) {
            String pkColumn = tableMeteColumn.get("COLUMN_NAME").toString();
            Range range = new Range();
            range.setDbTableName(dbTableName);
            long maxDiffTemp = 0;
            Map<String, Object> infoMap = getMaxDifference(pkColumn, dbTableName);
            Long difference = (Long) infoMap.get("difference");
            int min = (Integer) infoMap.get("min");
            int max = (Integer) infoMap.get("max");
            if (difference > maxDiffTemp) {
                range.setColumnName(pkColumn);
                range.setMaxId(max);
                range.setMinId(min);
                maxDiffTemp = difference;
            }
            rangeList = getRangeList(range, 3);
            System.out.println(rangeList);
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

        String tableName = dbTableName.split("\\.", 2)[1];
        long difference = 0;
        int min = 0;
        int max = 0;
        try {
            min = jdbcTemplate.queryForObject("select min(" + intColumnName + ") from " + tableName + "", Integer.class);
            max = jdbcTemplate.queryForObject("select max(" + intColumnName + ") from " + tableName + "", Integer.class);
        } catch (Exception e) {
            Log.error("切分表失败" + e.getMessage());
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
        long rangeNum = (max - min + 1) / splitNum;
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
