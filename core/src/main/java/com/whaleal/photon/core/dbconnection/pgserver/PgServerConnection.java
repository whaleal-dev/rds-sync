package com.whaleal.photon.core.dbconnection.pgserver;

import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.common.util.Log;

import java.sql.Connection;
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
    private static final Map<String, Connection> PG_CONNECTION_MAP = new ConcurrentHashMap<>();
    private static final Map<String, JdbcTemplate> JDBC_TEMPLATE_PG_MAP = new ConcurrentHashMap<>();

    /**
     * 根据数据库的名字或者数据源来获取连接
     *
     * @param dsName ds的名字
     * @param datasource                  数据源
     * @return {@link Connection}
     */
    public static void createConnection(String dsName, Datasource datasource) {
        if (!JDBC_TEMPLATE_PG_MAP.containsKey(dsName)) {
            synchronized (PgServerConnection.class) {
                if (!JDBC_TEMPLATE_PG_MAP.containsKey(dsName)) {
                    getBasicDataSource(dsName, datasource);
                }
            }
        }
    }

    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return JDBC_TEMPLATE_PG_MAP.get(dsName);
    }


    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return PG_CONNECTION_MAP.get(dsName);
    }

    /**
     * 根据datasource获取数据库的连接
     *
     * @param datasource 数据源
     * @return {@link Connection}
     */
    public static void getBasicDataSource(String dsName, Datasource datasource) {
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("org.postgresql.Driver");
            basicDataSource.setUrl(datasource.getUrl() + "?useCursorFetch=true");
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            JDBC_TEMPLATE_PG_MAP.put(dsName, new JdbcTemplate(basicDataSource));
            PG_CONNECTION_MAP.put(dsName, basicDataSource.getConnection());
        } catch (Exception e) {
            Log.error("链接PG:" + datasource.getName() + "数据源出现异常,错误信息:" + e.getMessage());
        }
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        if (!PG_CONNECTION_MAP.containsKey(dsName)) {
            return;
        }
        try {
            PG_CONNECTION_MAP.get(dsName).close();
        } catch (Exception e) {
            Log.error("关闭PG客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            PG_CONNECTION_MAP.remove(dsName);
            JDBC_TEMPLATE_PG_MAP.remove(dsName);
            Log.info("成功关闭PG链接:" + dsName);
        }
    }

    public static void main(String[] args) {
        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));
    }
}
