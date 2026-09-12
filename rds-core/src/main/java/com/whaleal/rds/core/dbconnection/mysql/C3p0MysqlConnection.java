package com.whaleal.rds.core.dbconnection.mysql;


import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.whaleal.rds.common.common.syncerV.entity.Datasource;
import com.whaleal.rds.common.util.Log;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @description:
 * @author: lhp
 * @time: 2021/12/24 10:29 上午
 */

public class C3p0MysqlConnection {


    private static final Map<String, Datasource> BASIC_DATA_SOURCE_MYSQL_MAP = new ConcurrentHashMap<>();

    private static final Map<String, ComboPooledDataSource> COMBO_POOLED_DATA_SOURCE_MYSQL_MAP = new ConcurrentHashMap<>();


    private static synchronized void getBasicDataSource(String dsName, Datasource datasource) {
        if (COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.containsKey(dsName)) {
            return;
        }
        try {
            ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
            comboPooledDataSource.setJdbcUrl(datasource.getUrl());
            comboPooledDataSource.setDriverClass(datasource.getJdbcDriverClass());
            comboPooledDataSource.setUser(datasource.getUsername());
            comboPooledDataSource.setPassword(datasource.getPassword());
            comboPooledDataSource.setInitialPoolSize(3);
            comboPooledDataSource.setMaxPoolSize(10);
            comboPooledDataSource.setMaxIdleTime(10000);
            COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.put(dsName, comboPooledDataSource);
            BASIC_DATA_SOURCE_MYSQL_MAP.put(dsName, datasource);
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
        if (!COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.containsKey(dsName)) {
            synchronized (MySqlConnection.class) {
                if (!COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.containsKey(dsName)) {
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
        Datasource datasource = BASIC_DATA_SOURCE_MYSQL_MAP.get(dsName);
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(datasource.getUrl());
        basicDataSource.setUsername(datasource.getUsername());
        basicDataSource.setPassword(datasource.getPassword());
        return new JdbcTemplate(basicDataSource);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        Connection connection = null;
        try {
            connection = COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.get(dsName).getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
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
        if (!COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.containsKey(dsName)) {
            return;
        }
        try {
            COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.get(dsName).close();
        } catch (Exception e) {
            Log.error("关闭MYSQL客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            COMBO_POOLED_DATA_SOURCE_MYSQL_MAP.remove(dsName);
            Log.info("成功关闭MYSQL链接:" + dsName);
        }
    }


}
