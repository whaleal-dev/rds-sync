package dbconnection.oracle;

import common.photonV.entity.Datasource;
import datasource.DataSourceUtil;
import dbconnection.mysql.MySqlConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import java.sql.Connection;
import java.sql.SQLException;
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
    private static Map<String, Connection> oracleConnectionMap = new ConcurrentHashMap<>();
    private static Map<String, JdbcTemplate> jdbcTemplateOracleMap = new ConcurrentHashMap<>();

    /**
     * 根据数据库的名字或者数据源来获取连接
     *
     * @param procNameAndBatchNoAndDsName     ds的名字
     * @param datasource 数据源
     */
    public static void createConnection(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (!jdbcTemplateOracleMap.containsKey(procNameAndBatchNoAndDsName)) {
            synchronized (OracleConnection.class) {
                if (!jdbcTemplateOracleMap.containsKey(procNameAndBatchNoAndDsName)) {
                    getBasicDataSource(procNameAndBatchNoAndDsName, datasource);
                }
            }
        }
    }

    public static JdbcTemplate getJdbcTemplate(String procNameAndBatchNoAndDsName) {
        return jdbcTemplateOracleMap.get(procNameAndBatchNoAndDsName);
    }


    /**
     * getJdbcTemplate 获取oracle的Jdbc
     *
     * @param procNameAndBatchNoAndDsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String procNameAndBatchNoAndDsName) {
        return oracleConnectionMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * 根据datasource获取数据库的连接
     *
     * @param datasource 数据源
     */
    public static void getBasicDataSource(String procNameAndBatchNoAndDsName, Datasource datasource) {
        try {
            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
            basicDataSource.setUrl(datasource.getUrl());
            basicDataSource.setUsername(datasource.getUsername());
            basicDataSource.setPassword(datasource.getPassword());
            jdbcTemplateOracleMap.put(procNameAndBatchNoAndDsName, new JdbcTemplate(basicDataSource));
            oracleConnectionMap.put(procNameAndBatchNoAndDsName, basicDataSource.getConnection());
            System.out.println("成功连接数据库"+procNameAndBatchNoAndDsName);
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.error(exception.getMessage());

        }
    }

    /**
     * close 关闭jdbc链接
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 关闭jdbc链接
     */
    public static void close(String procNameAndBatchNoAndDsName) {
        if (!oracleConnectionMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            oracleConnectionMap.get(procNameAndBatchNoAndDsName).close();
        } catch (Exception exception) {
            Log.error(exception.getMessage());
            exception.printStackTrace();
        } finally {
            oracleConnectionMap.remove(procNameAndBatchNoAndDsName);
            jdbcTemplateOracleMap.remove(procNameAndBatchNoAndDsName);
            Log.info(procNameAndBatchNoAndDsName+",oracle链接已关闭");
        }

    }

    public static void main(String[] args) {

        createConnection("pg", DataSourceUtil.getDataSourceByDsName("pg"));


    }


}
