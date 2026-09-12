package com.whaleal.rds.core.dbconnection.mysql;


import com.whaleal.rds.common.common.syncerV.entity.Datasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.rds.common.util.Log;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mysql链接类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class MySqlConnection {

    private static final Map<String, JdbcTemplate> JDBC_TEMPLATE_MYSQL_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Connection> CONNECTION_MYSQL_MAP = new ConcurrentHashMap<>();


    private static synchronized void getBasicDataSource(String dsName, Datasource datasource) {
        if (JDBC_TEMPLATE_MYSQL_MAP.containsKey(dsName)) {
            return;
        }
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
            JDBC_TEMPLATE_MYSQL_MAP.put(dsName, jdbcTemplate);
            CONNECTION_MYSQL_MAP.put(dsName, basicDataSource.getConnection());
        } catch (Exception e) {
            Log.error("链接MYSQL:" + datasource.getName() + "数据源出现异常,错误信息:" + e.getMessage());
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
        if (!JDBC_TEMPLATE_MYSQL_MAP.containsKey(dsName)) {
            synchronized (MySqlConnection.class) {
                if (!JDBC_TEMPLATE_MYSQL_MAP.containsKey(dsName)) {
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
        return JDBC_TEMPLATE_MYSQL_MAP.get(dsName);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return CONNECTION_MYSQL_MAP.get(dsName);
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        if (!CONNECTION_MYSQL_MAP.containsKey(dsName)) {
            return;
        }
        try {
            CONNECTION_MYSQL_MAP.get(dsName).close();

        } catch (SQLException e) {
            Log.error("关闭MYSQL客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            CONNECTION_MYSQL_MAP.remove(dsName);
            JDBC_TEMPLATE_MYSQL_MAP.remove(dsName);
            Log.info("成功关闭MYSQL链接:" + dsName);
        }
    }
}
