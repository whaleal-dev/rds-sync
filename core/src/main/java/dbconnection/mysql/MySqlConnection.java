package dbconnection.mysql;


import common.photonV.entity.Datasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mysql链接类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class MySqlConnection {

    private static Map<String, JdbcTemplate> jdbcTemplateMysqlMap = new ConcurrentHashMap<>();

    private static Map<String, Connection> connectionMysqlMap = new ConcurrentHashMap<>();


    private static synchronized void getBasicDataSource(String dsName, Datasource datasource) {
        if (jdbcTemplateMysqlMap.containsKey(dsName)) {
            return;
        }
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
            jdbcTemplateMysqlMap.put(dsName, jdbcTemplate);
            connectionMysqlMap.put(dsName, basicDataSource.getConnection());
        } catch (SQLException exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * getJdbcTemplate 获取mysql的connection
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static void createConnection(String dsName, Datasource datasource) {
        if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
            synchronized (MySqlConnection.class) {
                if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
                    getBasicDataSource(dsName, datasource);
                }
            }
        }
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return jdbcTemplateMysqlMap.get(dsName);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return connectionMysqlMap.get(dsName);
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        try {
            connectionMysqlMap.get(dsName).close();
            System.out.println(dsName + "数据源关闭");
        } catch (SQLException exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            connectionMysqlMap.remove(dsName);
            jdbcTemplateMysqlMap.remove(dsName);
        }
    }
}