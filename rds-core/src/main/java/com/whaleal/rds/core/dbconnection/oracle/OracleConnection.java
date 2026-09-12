package com.whaleal.rds.core.dbconnection.oracle;

import com.whaleal.rds.common.common.syncerV.entity.Datasource;
import com.whaleal.rds.core.datasource.DataSourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.rds.common.util.Log;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * oracle连接
 *
 * @author cs
 * @date 2021/08/30
 */
@Slf4j
public final class OracleConnection {
    private static final Map<String, Connection> ORACLE_CONNECTION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, JdbcTemplate> JDBC_TEMPLATE_ORACLE_MAP = new ConcurrentHashMap<>();

    /**
     * 根据数据库的名字或者数据源来获取连接
     *
     * @param dsName     ds的名字
     * @param datasource 数据源
     */
    public static void createConnection(String dsName, Datasource datasource) {
        if (!JDBC_TEMPLATE_ORACLE_MAP.containsKey(dsName)) {
            synchronized (OracleConnection.class) {
                if (!JDBC_TEMPLATE_ORACLE_MAP.containsKey(dsName)) {
                    getBasicDataSource(dsName, datasource);
                }
            }
        }
    }

    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return JDBC_TEMPLATE_ORACLE_MAP.get(dsName);
    }


    /**
     * getJdbcTemplate 获取oracle的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return ORACLE_CONNECTION_MAP.get(dsName);
    }

    /**
     * 根据datasource获取数据库的连接
     *
     * @param datasource 数据源
     */
    public static void getBasicDataSource(String dsName, Datasource datasource) {
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            JDBC_TEMPLATE_ORACLE_MAP.put(dsName, new JdbcTemplate(basicDataSource));
            ORACLE_CONNECTION_MAP.put(dsName, basicDataSource.getConnection());
        } catch (Exception e) {
            Log.error("链接ORACLE:" + datasource.getName() + "数据源出现异常,错误信息:" + e.getMessage());
        }
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        if (!ORACLE_CONNECTION_MAP.containsKey(dsName)) {
            return;
        }
        try {
            ORACLE_CONNECTION_MAP.get(dsName).close();
        } catch (Exception e) {
            Log.error("关闭ORACLE客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            ORACLE_CONNECTION_MAP.remove(dsName);
            JDBC_TEMPLATE_ORACLE_MAP.remove(dsName);
            Log.info("成功关闭ORACLE链接:" + dsName);
        }

    }

    public static void main(String[] args) {

        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));


    }


}
