package com.whaleal.photon.core.dbconnection.mysql;


import com.whaleal.photon.common.common.photonV.entity.Datasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.common.util.Log;

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

    private static Map<String, JdbcTemplate> jdbcTemplateMysqlMap = new ConcurrentHashMap<>();

    private static Map<String, Connection> connectionMysqlMap = new ConcurrentHashMap<>();


    private static synchronized void getBasicDataSource(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (jdbcTemplateMysqlMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
            jdbcTemplateMysqlMap.put(procNameAndBatchNoAndDsName, jdbcTemplate);
            connectionMysqlMap.put(procNameAndBatchNoAndDsName, basicDataSource.getConnection());
        } catch (SQLException exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * getJdbcTemplate 获取mysql的connection
     *
     * @param procNameAndBatchNoAndDsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static void createConnection(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (!jdbcTemplateMysqlMap.containsKey(procNameAndBatchNoAndDsName)) {
            synchronized (MySqlConnection.class) {
                if (!jdbcTemplateMysqlMap.containsKey(procNameAndBatchNoAndDsName)) {
                    getBasicDataSource(procNameAndBatchNoAndDsName, datasource);
                }
            }
        }
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param procNameAndBatchNoAndDsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static JdbcTemplate getJdbcTemplate(String procNameAndBatchNoAndDsName) {
        return jdbcTemplateMysqlMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param procNameAndBatchNoAndDsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String procNameAndBatchNoAndDsName) {
        return connectionMysqlMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * close 关闭jdbc链接
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 关闭jdbc链接
     */
    public static void close(String procNameAndBatchNoAndDsName) {
        if (!connectionMysqlMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            connectionMysqlMap.get(procNameAndBatchNoAndDsName).close();

        } catch (SQLException exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            connectionMysqlMap.remove(procNameAndBatchNoAndDsName);
            jdbcTemplateMysqlMap.remove(procNameAndBatchNoAndDsName);
            Log.info(procNameAndBatchNoAndDsName+",Mysql链接已关闭");
        }
    }
}