package dbconnection.mysql;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.client.MongoClient;
import common.photonV.entity.Datasource;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.*;

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
                connectionMysqlMap.get(dsName).close();
                // jdbcTemplateMysqlMap.get(dsName).DataSourceUtil().getConnection().close();
                System.out.println(dsName + "数据源关闭");
            } catch (SQLException exception) {
                Log.error(exception.getMessage());
                exception.printStackTrace();
            } finally {
                connectionMysqlMap.remove(dsName);
                jdbcTemplateMysqlMap.remove(dsName);
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


        getJdbcTemplate("1", datasource);
    }

    public static void closeDBResources(ResultSet rs, Statement stmt,
                                        Connection conn) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException unused) {
            }
        }

        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException unused) {
            }
        }

        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException unused) {
            }
        }
    }

    public static ResultSet query(Connection conn, String sql, int fetchSize)
            throws SQLException {
        // 默认3600 s 的query Timeout
        return query(conn, sql, fetchSize, 172800);
    }

    public static ResultSet query(Connection conn, String sql, int fetchSize, int queryTimeout)
            throws SQLException {
        // make sure autocommit is off
        conn.setAutoCommit(false);
        // ？
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(fetchSize);
        stmt.setQueryTimeout(queryTimeout);
        return query(stmt, sql);
    }

    public static ResultSet query(Statement stmt, String sql)
            throws SQLException {
        return stmt.executeQuery(sql);
    }

    public static boolean asyncResultSetNext(final ResultSet resultSet) {
        return asyncResultSetNext(resultSet, 3600);
    }

    public static boolean asyncResultSetNext(final ResultSet resultSet, int timeout) {
        Future<Boolean> future = rsExecutors.get().submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return resultSet.next();
            }
        });
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return false;
    }

    private static final ThreadLocal<ExecutorService> rsExecutors = new ThreadLocal<ExecutorService>() {
        @Override
        protected ExecutorService initialValue() {
            return Executors.newFixedThreadPool(1, new ThreadFactoryBuilder()
                    .setNameFormat("rsExecutors-%d")
                    .setDaemon(true)
                    .build());
        }
    };
}