package dbconnection.mysql;


import com.mongodb.client.MongoClient;
import common.photonV.entity.Datasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

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

    /**
     * getBasicDataSource 获取mysql的源链接
     *
     * @param dsName
     * @desc 获取mysql的源链接
     */
    private static synchronized void getBasicDataSource(String dsName) {
        if (jdbcTemplateMysqlMap.containsKey(dsName)) {
            return;
        }
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://192.168.3.19:3306/photon?useUnicode=true&characterEncoding=utf-8");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("123456");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
        jdbcTemplateMysqlMap.put(dsName, jdbcTemplate);
        try {
            connectionMysqlMap.put(dsName, basicDataSource.getConnection());
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        System.out.println(dsName + "数据源启动成功");
    }

    private static synchronized void getBasicDataSource(String dsName, Datasource datasource) {
        if (jdbcTemplateMysqlMap.containsKey(dsName)) {
            return;
        }
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(datasource.getUrl());
        basicDataSource.setUsername(datasource.getUsername());
        basicDataSource.setPassword(datasource.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
        jdbcTemplateMysqlMap.put(dsName, jdbcTemplate);
        try {
            connectionMysqlMap.put(dsName, basicDataSource.getConnection());
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * getJdbcTemplate 获取mysql的connection
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName) {
        if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
            getBasicDataSource(dsName);
        }
        synchronized (MySqlConnection.class) {
            if (!connectionMysqlMap.containsKey(dsName)) {
                try {
                    connectionMysqlMap.put(dsName, jdbcTemplateMysqlMap.get(dsName).getDataSource().getConnection());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }
        return connectionMysqlMap.get(dsName);
    }

    /**
     * getJdbcTemplate 获取mysql的connection
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static Connection getConnection(String dsName, Datasource datasource) {
        if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
            getBasicDataSource(dsName, datasource);
        }
        synchronized (MySqlConnection.class) {
            if (!connectionMysqlMap.containsKey(dsName)) {
                try {
                    connectionMysqlMap.put(dsName, jdbcTemplateMysqlMap.get(dsName).getDataSource().getConnection());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        }
        return connectionMysqlMap.get(dsName);
    }

    /**
     * getJdbcTemplate 获取mysql的Jdbc
     *
     * @param dsName
     * @return JdbcTemplate
     * @desc 获取mysql的Jdbc
     */
    public static JdbcTemplate getJdbcTemplate(String dsName) {
        if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
            getBasicDataSource(dsName);
        }
        return jdbcTemplateMysqlMap.get(dsName);
    }

    public static JdbcTemplate getJdbcTemplate(String dsName, Datasource datasource) {
        if (!jdbcTemplateMysqlMap.containsKey(dsName)) {
            getBasicDataSource(dsName, datasource);
        }
        return jdbcTemplateMysqlMap.get(dsName);
    }

    /**
     * close 关闭jdbc链接
     *
     * @param dsName
     * @desc 关闭jdbc链接
     */
    public static void close(String dsName) {
        if (jdbcTemplateMysqlMap.containsKey(dsName)) {
            try {
                jdbcTemplateMysqlMap.get(dsName).getDataSource().getConnection().close();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        //getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='mysql1' ");

        Datasource datasource = new Datasource();
        datasource.setId(map.get("id").toString());
        datasource.setName(map.get("name").toString());
        datasource.setType(map.get("type").toString());
        datasource.setDsDatabase(map.get("ds_database").toString());
        datasource.setUsername(map.get("username").toString());
        datasource.setPassword(map.get("password").toString());

        datasource.setId(map.get("ip").toString());


        datasource.setUrl(map.get("url").toString());

        datasource.setPort((String) map.get("port"));


        getJdbcTemplate("1",datasource);
    }
}