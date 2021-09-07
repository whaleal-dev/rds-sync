package com.whaleal.photon.source.oracle.split;


import common.dataclass.Range;
import common.photonV.entity.Datasource;
import common.taskbase.SplitRangeOfRdbInterface;
import datasource.DataSourceUtil;
import dbconnection.oracle.OracleConnection;
import dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;
import util.split.RangeSplitUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

/**
 * @author: lhp
 * @time: 2021/7/16 3:04 下午
 * @desc:
 */
public class OracleSourceSplitRange implements SplitRangeOfRdbInterface {

    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public OracleSourceSplitRange(String sourceDsName) {
        this.sourceDsName = sourceDsName;
        this.jdbcTemplate = OracleConnection.getJdbcTemplate(sourceDsName);
    }

    @Override
    public Map<String, Object> getIntMaxDifference(String intColumnName, String dbTableName) {
        return null;
    }

    public Map<String, Object> getDecimalMaxDifference(String decimalColumnName, String dbTableName) {
        String tableName = dbTableName.split("\\.", 2)[1];
        BigDecimal difference = new BigDecimal(0);
        BigDecimal min = new BigDecimal(0);
        BigDecimal max = new BigDecimal(0);
        String sql = "select max(" + decimalColumnName + ") max ,min(" + decimalColumnName + ") min from " + tableName;
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                max = (BigDecimal) mapList.get(0).get("max");
                min = (BigDecimal) mapList.get(0).get("min");
            }
        } catch (Exception e) {
            //e.printStackTrace();
            Log.error(e.getMessage());
            min = new BigDecimal(0);
            max = new BigDecimal(0);
        }
        difference = max.subtract(min);
        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("min", min);
        infoMap.put("max", max);
        infoMap.put("difference", difference);
        return infoMap;
    }

    @Override
    public Map<String, Object> getStringLengthMaxDifference(String stringColumnName, String dbTableName) {
        String tableName = dbTableName.split("\\.", 2)[1];
        BigDecimal difference = new BigDecimal(0);
        BigDecimal min = new BigDecimal(0);
        BigDecimal max = new BigDecimal(0);
        String sql = "select max(length(" + stringColumnName + ")) max,min(length(" + stringColumnName + ")) min from  " + tableName + "";
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                max = (BigDecimal) mapList.get(0).get("max");
                min = (BigDecimal) mapList.get(0).get("min");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
            min = new BigDecimal(0);
            max = new BigDecimal(0);
        }
        difference = max.subtract(min);
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
        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(sql, tableName);
        Set<String> decimalColumnSet = new HashSet<>();
        Set<String> stringColumnSet = new HashSet<>();
        for (Map<String, Object> columnMap : tableMeteColumn) {
            if (columnMap.get("DATA_TYPE").toString().toUpperCase().contains("NUMBER")) {
                decimalColumnSet.add(columnMap.get("COLUMN_NAME").toString());
            }
            if (columnMap.get("DATA_TYPE").toString().toUpperCase().contains("CHAR")) {
                stringColumnSet.add(columnMap.get("COLUMN_NAME").toString());
            }
        }
        //先decimal
        if (decimalColumnSet.size() != 0) {
            rangeList = generateRangeListByDecimalColumn(decimalColumnSet, dbTableName);
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
        try {

            String sql = "select a.*  from user_tab_columns A where TABLE_NAME=? ";
            rangeList = getRangeList(dbTableName, sql);
        } catch (Exception e) {

        }
        if (rangeList.size() == 0) {
            Range range = new Range();
            range.setDbTableName(dbTableName);
            range.setQuery("(1=1)");
            rangeList.add(range);
        }
        return rangeList;
    }

    @Override
    public List<Range> generateRangeListByIntColumn(Set<String> stringColumnSet, String dbTableName) {
        return null;
    }


    public List<Range> generateRangeListByDecimalColumn(Set<String> decimalColumnSet, String dbTableName) {
        List<Range> rangeList = new ArrayList<>();
        Range range = new Range();
        range.setDbTableName(dbTableName);
        BigDecimal maxDiffTemp = new BigDecimal(0);
        for (String decimalColumnName : decimalColumnSet) {
            Map<String, Object> infoMap = getDecimalMaxDifference(decimalColumnName, dbTableName);
            BigDecimal difference = (BigDecimal) infoMap.get("difference");
            BigDecimal min = (BigDecimal) infoMap.get("min");
            BigDecimal max = (BigDecimal) infoMap.get("max");
            if (difference.compareTo(maxDiffTemp) > 0) {
                range.setColumnName(decimalColumnName);
                range.setMaxId(max);
                range.setMinId(min);
                maxDiffTemp = difference;
            }
        }
        System.out.println(range);
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByLongType(((BigDecimal) range.getMinId()).longValue(), ((BigDecimal) range.getMaxId()).longValue(), 3, range.getColumnName());
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
        BigDecimal maxDiffTemp = new BigDecimal(0);
        for (String stringColumnName : stringColumnSet) {
            Map<String, Object> infoMap = getStringLengthMaxDifference(stringColumnName, dbTableName);
            BigDecimal difference = (BigDecimal) infoMap.get("difference");
            BigDecimal min = (BigDecimal) infoMap.get("min");
            BigDecimal max = (BigDecimal) infoMap.get("max");
            if (difference.compareTo(maxDiffTemp) > 0) {
                range.setColumnName(stringColumnName);
                range.setMaxId(max);
                range.setMinId(min);
                maxDiffTemp = difference;
            }
        }
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByStringLengthType(
                ((BigDecimal) range.getMinId()).longValue(),
                ((BigDecimal) range.getMaxId()).longValue(), 3, range.getColumnName());
        for (Range rangeIndex : rangeList) {
            rangeIndex.setDbTableName(dbTableName);
        }
        return rangeList;
    }


    public static void main(String[] args) {
        Datasource oracle = DataSourceUtil.getDataSourceByDsName("oracle3");
        OracleConnection.createConnection(oracle.getName(), oracle);
        PgServerConnection.getConnection("oracle3");
        OracleSourceSplitRange oracleSourceSplitRangeByLhp = new OracleSourceSplitRange("oracle3");
        oracleSourceSplitRangeByLhp.getRangeList("public.TEST_P_2").forEach(range -> System.out.println(range.getQuery()));
        // oracleSourceSplitRangeByLhp.getStringLengthMaxDifference("T_NAME", "cs.TTYPE");

    }
}
