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



    }



}
