package com.whaleal.photon.source.mysql.sourcesplit;

import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.common.taskbase.SplitRangeOfRdbInterface;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.common.util.split.RangeSplitUtil;

import java.util.*;

/**
 * @author liheping
 */
public class MysqlSourceSplitRangeOfLHP implements SplitRangeOfRdbInterface {

    private JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public MysqlSourceSplitRangeOfLHP(String sourceDsName, String procName, long batchNo) {
        this.sourceDsName = sourceDsName;
        String procNameAndBatchNoAndSourceDsName = procName + batchNo+sourceDsName;
        this.jdbcTemplate = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndSourceDsName);
    }

    @Override
    public Map<String, Object> getIntMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0L;
        long min = 0;
        long max = 0;
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
           // e.printStackTrace();
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
    public Map<String, Object> getStringLengthMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0L;
        long min = 0L;
        long max = 0L;
        String sql = "select max(length(" + intColumnName + ")) max ,min(length(" + intColumnName + ")) min from  " + dbTableName + "";
        try {
            List<Map<String, Object>> mapList = jdbcTemplate.queryForList(sql);
            if (mapList.size() != 0) {
                Object minTemp = mapList.get(0).get("min");
                Object maxTemp = mapList.get(0).get("max");
                if (maxTemp.getClass().getSimpleName().toUpperCase().equalsIgnoreCase("Integer")) {
                    max = (Integer) maxTemp;
                } else {
                    max = (Long) maxTemp;
                }

                if (minTemp.getClass().getSimpleName().toUpperCase().equalsIgnoreCase("Integer")) {
                    min = (Integer) minTemp;
                } else {
                    min = (Long) minTemp;
                }
            }
        } catch (Exception e) {
          //  e.printStackTrace();
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
    public List<Range> getRangeList(String dbTableName, String sql) {
        List<Range> rangeList = new ArrayList<>();
        String[] split = dbTableName.split("\\.", 2);
        String dbName = split[0];
        String tableName = split[1];
        List<Map<String, Object>> tableMeteColumn = jdbcTemplate.queryForList(sql, dbName, tableName);
        System.out.println(sql);
        Set<String> intColumnSet = new HashSet<>();
        Set<String> stringColumnSet = new HashSet<>();
        for (Map<String, Object> columnMap : tableMeteColumn) {
           // System.out.println(columnMap);
            if ("INTEGER".equalsIgnoreCase(columnMap.get("DATA_TYPE").toString())) {
                intColumnSet.add(columnMap.get("COLUMN_NAME").toString());
            }
            if (columnMap.get("DATA_TYPE").toString().toUpperCase().contains("CHAR")) {
                stringColumnSet.add(columnMap.get("COLUMN_NAME").toString());
            }
        }
      //  System.out.println(stringColumnSet);
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
        String sqlOfPrimary = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema=? AND t.table_name =? and t.column_key  is not null  order by ordinal_position ";
        rangeList = getRangeList(dbTableName, sqlOfPrimary);
        if (rangeList.size() == 0) {
            String sqlOfOrdinary = "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_schema=? AND table_name =? order by ordinal_position ";
            rangeList = getRangeList(dbTableName, sqlOfOrdinary);
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
    public List<Range> generateRangeListByIntColumn(Set<String> intColumnSet, String dbTableName) {
        List<Range> rangeList = new ArrayList<>();
        Range range = new Range();
        range.setDbTableName(dbTableName);
        long maxDiffTemp = 0;
        for (String intColumnName : intColumnSet) {
            Map<String, Object> infoMap = getIntMaxDifference(intColumnName, dbTableName);
            Long difference = (Long) infoMap.get("difference");
            long min = (Integer) infoMap.get("min");
            long max = (Integer) infoMap.get("max");
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
        rangeList = RangeSplitUtil.getRangeListByLongType((int) range.getMinId(), (int) range.getMaxId(), 3, range.getColumnName());
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
            long difference = (long) infoMap.get("difference");
            long min = (long) infoMap.get("min");
            long max = (long) infoMap.get("max");
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
        Datasource mysqltestDb = DataSourceUtil.getDataSourceByDsName("mysqltest");
        MySqlConnection.createConnection(mysqltestDb.getName(), mysqltestDb);
        MysqlSourceSplitRangeOfLHP mysqlSourceSplitRangeOfLHP = new MysqlSourceSplitRangeOfLHP("mysqltest","",1);

        List<Map<String, Object>> mapList = mysqlSourceSplitRangeOfLHP.jdbcTemplate.queryForList("show tables");
//         //       forEach(stringObjectMap -> System.out.println(stringObjectMap.get("Tables_in_community")));

        for(Map map:mapList){
            String tableName=map.get("Tables_in_community").toString();
            mysqlSourceSplitRangeOfLHP.getRangeList("community."+tableName).forEach(range -> System.out.println(tableName+"          =====           "+range.getQuery()));
        }


    }

}
