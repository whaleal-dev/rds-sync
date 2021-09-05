package dbconnection.pgserver;

import common.dataclass.Range;
import common.photonV.entity.Datasource;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
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
    public static void createConnection(String dsName, Datasource datasource) {
        if (!jdbcTemplatePgMap.containsKey(dsName)) {
            synchronized (PgServerConnection.class) {
                if (!jdbcTemplatePgMap.containsKey(dsName)) {
                    getBasicDataSource(dsName, datasource);
                }
            }
        }
    }

    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return jdbcTemplatePgMap.get(dsName);
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
    public static void getBasicDataSource(String dsName, Datasource datasource) {
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("org.postgresql.Driver");
            basicDataSource.setUrl(datasource.getUrl()+"?useCursorFetch=true");
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            System.out.println("成功连接数据库");
            jdbcTemplatePgMap.put(dsName, new JdbcTemplate(basicDataSource));
            pgConnectionMap.put(dsName, basicDataSource.getConnection());
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        try {
            pgConnectionMap.get(dsName).close();
            System.out.println(dsName + "数据源关闭");
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            pgConnectionMap.remove(dsName);
            jdbcTemplatePgMap.remove(dsName);
        }
    }

    public static void main(String[] args) {

        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));


    }


}
