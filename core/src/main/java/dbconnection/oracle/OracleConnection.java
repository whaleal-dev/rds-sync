package dbconnection.oracle;

import common.photonV.entity.Datasource;
import datasource.DBUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
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
     * @param dsName     ds的名字
     * @param datasource 数据源
     * @return {@link Connection}
     */
    public static Connection createConnection(String dsName, Datasource datasource) {
        if (!oracleConnectionMap.containsKey(dsName)) {
            return oracleConnectionMap.get(dsName);
        }
        Connection connection = null;
        synchronized (OracleConnection.class) {
            if (!oracleConnectionMap.containsKey(dsName)) {
                connection = createConnection(datasource);
                oracleConnectionMap.put(dsName, connection);
            }
            return connection;
        }
    }

    public static JdbcTemplate getJdbcTemplate(String dsName) {
        return jdbcTemplateOracleMap.get(dsName);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        return oracleConnectionMap.get(dsName);
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
            Class.forName("oracle.jdbc.OracleDriver");
            connection = DriverManager.getConnection(datasource.getUrl(), datasource.getUsername(), datasource.getPassword());
            System.out.println("成功连接数据库");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class not find !", e);
        } catch (SQLException e) {
            throw new RuntimeException("get connection error!", e);
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
        if (oracleConnectionMap.containsKey(dsName)) {
            try {
                oracleConnectionMap.get(dsName).close();
                // jdbcTemplateOracleMap.get(dsName).DataSourceUtil().getConnection().close();
                System.out.println(dsName + "数据源关闭");
            } catch (SQLException exception) {
                Log.error(exception.getMessage());
                exception.printStackTrace();
            } finally {
                oracleConnectionMap.remove(dsName);
            }
        }
    }

//    public static void main(String[] args) {
//        //getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
//        //Map<String, Object> map = OracleConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='mysql1' ");
//        Datasource proc3 = DBUtil.getSourceByProcName("proc3");
//        Connection connection = getConnection(proc3);
//        System.out.println(connection);
//        System.out.println(proc3.getDsDatabase());
//
//    }
}
