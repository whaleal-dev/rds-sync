package com.whaleal.rds.source.pg.sourcesplit;


import com.whaleal.rds.common.common.dataclass.Range;
import com.whaleal.rds.common.common.syncerV.entity.Datasource;
import com.whaleal.rds.common.common.taskbase.SplitRangeOfRdbInterface;
import com.whaleal.rds.core.datasource.DataSourceUtil;
import com.whaleal.rds.core.dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.rds.common.util.Log;
import com.whaleal.rds.common.util.split.RangeSplitUtil;

import java.sql.Connection;
import java.util.*;

/**
 * @author: lhp
 * @time: 2021/7/16 3:04 下午
 * @desc:
 */
public class PgSourceSplitRange implements SplitRangeOfRdbInterface {

    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public PgSourceSplitRange(String sourceDsName, String procName, long batchNo) {
        this.sourceDsName = sourceDsName;
        String procNameAndBatchNoAndSourceDsName = procName + batchNo + sourceDsName;
        this.jdbcTemplate = PgServerConnection.getJdbcTemplate(procNameAndBatchNoAndSourceDsName);
    }

    @Override
    public Map<String, Object> getIntMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0L;
        long min = 0L;
        long max = 0L;
        String sql = "select min(" + intColumnName + ")  min, max(" + intColumnName + ")  max from " + dbTableName + "";
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                Object minTemp = mapList.get(0).get("min");
                Object maxTemp = mapList.get(0).get("max");
                if (maxTemp.getClass().getSimpleName().equalsIgnoreCase("Integer")) {
                    max = (Integer) maxTemp;
                } else {
                    max = (Long) maxTemp;
                }
                if (minTemp.getClass().getSimpleName().equalsIgnoreCase("Integer")) {
                    min = (Integer) minTemp;
                } else {
                    min = (Long) minTemp;
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            Log.error(e.getMessage());
            min = 0L;
            max = 0L;
        }
        difference = (max - min);
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("min", min);
        infoMap.put("max", max);
        infoMap.put("difference", difference);
        return infoMap;
    }

    @Override
    public Map<String, Object> getStringLengthMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0;
        long min = 0;
        long max = 0;
        String sql = "select max(length(" + intColumnName + ")),min(length(" + intColumnName + ")) from  " + dbTableName + "";
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                Object minTemp = mapList.get(0).get("min");
                Object maxTemp = mapList.get(0).get("max");
                System.out.println(maxTemp.getClass().getSimpleName());
                if (maxTemp.getClass().getSimpleName().equalsIgnoreCase("Integer")) {
                    max = (Integer) maxTemp;
                } else {
                    max = (Long) maxTemp;
                }
                if (minTemp.getClass().getSimpleName().equalsIgnoreCase("Integer")) {
                    min = (Integer) minTemp;
                } else {
                    min = (Long) minTemp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public List<Range> getRangeList(String dbTableName, String sql) {
        List<Range> rangeList = new ArrayList<>();
        String[] split = dbTableName.split("\\.", 2);
        String dbName = split[0];
        String tableName = split[1];
        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(sql, dbName, tableName);
        Set<String> intColumnSet = new HashSet<>();
        Set<String> stringColumnSet = new HashSet<>();
        for (Map<String, Object> columnMap : tableMeteColumn) {
            if (columnMap.get("data_type").toString().toUpperCase().contains("INT")) {
                intColumnSet.add(columnMap.get("column_name").toString());
            }
            if (columnMap.get("data_type").toString().toUpperCase().contains("CHAR")) {
                stringColumnSet.add(columnMap.get("column_name").toString());
            }
        }
        //先int
        if (intColumnSet.size() != 0) {
            rangeList = generateRangeListByIntColumn(intColumnSet, dbTableName);
        }
        //第二为String类型长度
        if (rangeList.size() == 0) {
            rangeList = generateRangeListByStringColumn(stringColumnSet, dbTableName);
        }
        return rangeList;
    }

    @Override
    public List<Range> getRangeList(String dbTableName) {
        List<Range> rangeList = new ArrayList<>();
        String sql = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema=? AND table_name =? order by ordinal_position ";
        rangeList = getRangeList(dbTableName, sql);
        if (rangeList.size() == 0) {
            Range range = new Range();
            range.setDbTableName(dbTableName);
            range.setQueryCondition("(1=1)");
            rangeList.add(range);
        }

        for (Range range : rangeList) {
            Object condition = range.getQueryCondition() != null ? range.getQueryCondition() : range.getQuery();
            String querySql = "select * from  " + dbTableName + " where " + condition;
            range.setQuery(querySql);
        }
        return rangeList;
    }

    @Override
    public List<Range> generateRangeListByIntColumn(Set<String> intColumnSet, String dbTableName) {
        List<Range> rangeList = new ArrayList<>();
        Range range = new Range();
        range.setDbTableName(dbTableName);
        long maxDiffTemp = 0;
        for (String intColumnName : intColumnSet) {
            Map<String, Object> infoMap = getIntMaxDifference(intColumnName, dbTableName);
            Long difference = (Long) infoMap.get("difference");
            long min = (long) infoMap.get("min");
            long max = (long) infoMap.get("max");
            if (difference > maxDiffTemp) {
                range.setColumnName(intColumnName);
                range.setMaxId(max);
                range.setMinId(min);
                maxDiffTemp = difference;
            }
        }
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByLongType((long) range.getMinId(), (long) range.getMaxId(), 3, range.getColumnName());
        for (Range rangeIndex : rangeList) {
            rangeIndex.setDbTableName(dbTableName);
        }
        System.out.println(range);
        return rangeList;
    }

    @Override
    public List<Range> generateRangeListByStringColumn(Set<String> stringColumnSet, String dbTableName) {
        List<Range> rangeList = new ArrayList<>();
        Range range = new Range();
        range.setDbTableName(dbTableName);
        long maxDiffTemp = 0;
        for (String stringColumnName : stringColumnSet) {
            Map<String, Object> infoMap = getStringLengthMaxDifference(stringColumnName, dbTableName);
            int difference = (int) infoMap.get("difference");
            int min = (Integer) infoMap.get("min");
            int max = (Integer) infoMap.get("max");
            if (difference > maxDiffTemp) {
                range.setColumnName(stringColumnName);
                range.setMaxId(max);
                range.setMinId(min);
                maxDiffTemp = difference;
            }
        }
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByStringLengthType((long) range.getMinId(), (long) range.getMaxId(), 3, range.getColumnName());
        for (Range rangeIndex : rangeList) {
            rangeIndex.setDbTableName(dbTableName);
        }
        return rangeList;
    }


    public static void main(String[] args) {
        Datasource pg = DataSourceUtil.getDataSourceByDsName("pg");
        PgServerConnection.createConnection(pg.getName(), pg);
        Connection pgConnection = PgServerConnection.getConnection("pg");
        PgSourceSplitRange pgSourceSplitRange = new PgSourceSplitRange("pg", "", 0);
        pgSourceSplitRange.getRangeList("public.primary").forEach(range -> System.out.println(range.getQuery()));
    }
}
