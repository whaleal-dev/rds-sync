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
     * @param procNameAndBatchNoAndDsName ds的名字
     * @param datasource                  数据源
     * @return {@link Connection}
     */
    public static void createConnection(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (!jdbcTemplatePgMap.containsKey(procNameAndBatchNoAndDsName)) {
            synchronized (PgServerConnection.class) {
                if (!jdbcTemplatePgMap.containsKey(procNameAndBatchNoAndDsName)) {
                    getBasicDataSource(procNameAndBatchNoAndDsName, datasource);
                }
            }
        }
    }

    public static JdbcTemplate getJdbcTemplate(String procNameAndBatchNoAndDsName) {
        return jdbcTemplatePgMap.get(procNameAndBatchNoAndDsName);
    }


    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param procNameAndBatchNoAndDsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String procNameAndBatchNoAndDsName) {
        return pgConnectionMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * 根据datasource获取数据库的连接
     *
     * @param datasource 数据源
     * @return {@link Connection}
     */
    public static void getBasicDataSource(String procNameAndBatchNoAndDsName, Datasource datasource) {
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("org.postgresql.Driver");
            basicDataSource.setUrl(datasource.getUrl() + "?useCursorFetch=true");
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            System.out.println("成功连接数据库");
            jdbcTemplatePgMap.put(procNameAndBatchNoAndDsName, new JdbcTemplate(basicDataSource));
            pgConnectionMap.put(procNameAndBatchNoAndDsName, basicDataSource.getConnection());
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * close 关闭jdbc链接
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 关闭jdbc链接
     */
    public static void close(String procNameAndBatchNoAndDsName) {
        if (!pgConnectionMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            pgConnectionMap.get(procNameAndBatchNoAndDsName).close();
            System.out.println(procNameAndBatchNoAndDsName + "数据源关闭");
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            pgConnectionMap.remove(procNameAndBatchNoAndDsName);
            jdbcTemplatePgMap.remove(procNameAndBatchNoAndDsName);
            Log.info(procNameAndBatchNoAndDsName + "链接已关闭");
        }
    }

    public static void main(String[] args) {

        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));


    }


}
