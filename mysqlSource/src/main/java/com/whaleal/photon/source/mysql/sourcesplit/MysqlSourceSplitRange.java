package com.whaleal.photon.source.mysql.sourcesplit;

import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.common.taskbase.SplitRangeOfRdbInterface;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.common.util.split.RangeSplitUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * @author liheping
 */
public class MysqlSourceSplitRange implements SplitRangeOfRdbInterface {

    private final JdbcTemplate jdbcTemplate;
    /**
     * 数据源名称
     */
    private final String sourceDsName;

    public MysqlSourceSplitRange(String sourceDsName, String procName) {
        this.sourceDsName = sourceDsName;
        this.jdbcTemplate = MySqlConnection.getJdbcTemplate(sourceDsName);
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
            Log.error("切分" + dbTableName + "表的INT字段" + intColumnName + "任务时发生错误,报错信息:" + e.getMessage());
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
    public Map<String, Object> getStringLengthMaxDifference(String strColumnName, String dbTableName) {
        long difference = 0L;
        long min = 0L;
        long max = 0L;
        String sql = "select max(length(" + strColumnName + ")) max ,min(length(" + strColumnName + ")) min from  " + dbTableName + "";
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
            Log.error("切分" + dbTableName + "表的STR字段" + strColumnName + "任务时发生错误,报错信息:" + e.getMessage());
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
        Set<String> intColumnSet = new HashSet<>();
        Set<String> stringColumnSet = new HashSet<>();
        for (Map<String, Object> columnMap : tableMeteColumn) {
            if ("INTEGER".equalsIgnoreCase(columnMap.get("DATA_TYPE").toString())) {
                intColumnSet.add(columnMap.get("COLUMN_NAME").toString());
            }
            if (columnMap.get("DATA_TYPE").toString().toUpperCase().contains("CHAR")) {
                stringColumnSet.add(columnMap.get("COLUMN_NAME").toString());
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
                range.setMaxValue(max);
                range.setMinValue(min);
                maxDiffTemp = difference;
            }
        }
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByLongType((int) range.getMinValue(), (int) range.getMaxValue(), 3, range.getColumnName());
        for (Range rangeIndex : rangeList) {
            rangeIndex.setDbTableName(dbTableName);
        }
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
                range.setMaxValue(max);
                range.setMinValue(min);
                maxDiffTemp = difference;
            }
        }
        if (range.getColumnName() == null || range.getColumnName().equals("")) {
            return rangeList;
        }
        rangeList = RangeSplitUtil.getRangeListByStringLengthType((long) range.getMinValue(), (long) range.getMaxValue(), 3, range.getColumnName());
        for (Range rangeIndex : rangeList) {
            rangeIndex.setDbTableName(dbTableName);
        }
        return rangeList;
    }


    public static void main(String[] args) {
        Datasource mysqltestDb = DataSourceUtil.getDataSourceByDsName("jmcMysql");
        MySqlConnection.createConnection(mysqltestDb.getName(), mysqltestDb);
        MysqlSourceSplitRange mysqlSourceSplitRange = new MysqlSourceSplitRange("jmcMysql", "");

        Set<String> dbTableSet = new HashSet<>();
        dbTableSet.add("dict");
        dbTableSet.add("dict_detail");
        dbTableSet.add("menu");
        dbTableSet.add("role");
        dbTableSet.add("roles_menus");
        dbTableSet.add("sms_country_code");
        dbTableSet.add("sms_country_code_api");
        dbTableSet.add("sms_country_user");
        dbTableSet.add("sms_country_user_api");
        dbTableSet.add("sms_deposit_log");
        dbTableSet.add("sms_key_pool");
        dbTableSet.add("sms_network");
        dbTableSet.add("sms_poc_key");
        dbTableSet.add("sms_poc_key_from");
        dbTableSet.add("sms_pre_deposit");
        dbTableSet.add("sms_signature");
        dbTableSet.add("sms_task");
        dbTableSet.add("sms_task_detail_log");
        dbTableSet.add("sms_task_detail_log_before");
        dbTableSet.add("sms_task_log");
        dbTableSet.add("sms_task_log_before");
        dbTableSet.add("sms_task_phone_number");
        dbTableSet.add(" sms_temp_upload_phone");
        dbTableSet.add("sms_temp_upload_phone_error");
        dbTableSet.add("sms_template");
        dbTableSet.add("sms_user_balance");
        dbTableSet.add("sms_user_from");
        dbTableSet.add("sms_vonage");
        dbTableSet.add("sms_webhook");
        dbTableSet.add("user");
        dbTableSet.add("users_roles");
        for (String tableName : dbTableSet) {
            List<Map<String, Object>> mapList = mysqlSourceSplitRange.jdbcTemplate.queryForList("show tables");
            System.out.println(mapList);
            mysqlSourceSplitRange.getRangeList("sms." + tableName).forEach(range -> Log.info(range.getQuery() + ""));

        }


//        Statement statement = null;
//        ResultSet resultSet = null;
//        try {
//            statement = MySqlConnection.getConnection(mysqltestDb.getName()).createStatement();
//            resultSet = statement.executeQuery("select * from sms.sms_task_detail_log limit 1000000");
//            while (resultSet.next()) {
//                System.out.println(resultSet.toString());
//            }
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        } finally {
//            try {
//                if (resultSet != null) {
//                    resultSet.close();
//                }
//                if (statement != null) {
//                    statement.close();
//                }
//            } catch (Exception e) {
//                Log.error(e.getMessage());
//            }
//
//        }

    }

}
