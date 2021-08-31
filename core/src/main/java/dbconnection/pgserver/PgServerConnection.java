package dbconnection.pgserver;

import common.dataclass.Range;
import common.photonV.entity.Datasource;
import datasource.DataSourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * oracle连接
 *
 * @author cs
 * @date 2021/08/30
 */
@Slf4j
public final class PgServerConnection {
    private static Map<String, Connection> pgConnectionMap = new ConcurrentHashMap<>();
    private static Map<String, JdbcTemplate> jdbcTemplatePgMap = new ConcurrentHashMap<>();

    /**
     * 根据数据库的名字或者数据源来获取连接
     *
     * @param dsName     ds的名字
     * @param datasource 数据源
     * @return {@link Connection}
     */
    public static Connection createConnection(String dsName, Datasource datasource) {
        if (pgConnectionMap.containsKey(dsName)) {
            return pgConnectionMap.get(dsName);
        }
        Connection connection = null;
        synchronized (PgServerConnection.class) {
            if (!pgConnectionMap.containsKey(dsName)) {
                connection = createConnection(datasource);
                pgConnectionMap.put(dsName, connection);

            }
            return connection;
        }
    }

    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return jdbcTemplatePgMap.get(dsName);
    }

    public static JdbcTemplate getJdbcTemplateBySource(Datasource datasource) {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(datasource.getUrl());
        basicDataSource.setUsername(datasource.getUsername());
        basicDataSource.setPassword(datasource.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
        return jdbcTemplate;
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return pgConnectionMap.get(dsName);
    }

    /**
     * 根据datasource获取数据库的连接
     *
     * @param datasource 数据源
     * @return {@link Connection}
     */
    public static synchronized Connection createConnection(Datasource datasource) {
        Connection connection = null;
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("org.postgresql.Driver");
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            System.out.println("成功连接数据库");
            jdbcTemplatePgMap.put(datasource.getName(), new JdbcTemplate(basicDataSource));
            connection = basicDataSource.getConnection();
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        }

        return connection;
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        if (pgConnectionMap.containsKey(dsName)) {
            try {
                pgConnectionMap.get(dsName).close();
                // jdbcTemplateOracleMap.get(dsName).DataSourceUtil().getConnection().close();
                System.out.println(dsName + "数据源关闭");
            } catch (SQLException exception) {
                Log.error(exception.getMessage());
                exception.printStackTrace();
            } finally {
                pgConnectionMap.remove(dsName);
                jdbcTemplatePgMap.remove(dsName);
            }
        }
    }

    public static void main(String[] args) {

        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));
        List<Map<String, Object>> tableMeteColumn = getJdbcTemplate("pg").queryForList(
                "SELECT column_name,data_type FROM information_schema.columns t WHERE t.table_catalog='test' AND table_name ='student' order by ordinal_position ");
        Set<String> intColumnSet = new HashSet<>();
        String dbTableName = "student";
        for (Map<String, Object> columnMap : tableMeteColumn) {
            if ("INTEGER".equalsIgnoreCase(columnMap.get("data_type").toString())) {
                intColumnSet.add(columnMap.get("column_name").toString());
            }
        }

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
        System.out.println(range);
        getRangeList(range, 3).forEach(range1 -> System.out.println(range1));
    }


    public static Map<String, Object> getMaxDifference(String intColumnName, String dbTableName) {
        long difference = 0;
        int min = 0;
        int max = 0;
        try {
            min = getJdbcTemplate("pg").queryForObject("select min(" + intColumnName + ") from " + dbTableName + "", Integer.class);
            max = getJdbcTemplate("pg").queryForObject("select max(" + intColumnName + ") from " + dbTableName + "", Integer.class);
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
