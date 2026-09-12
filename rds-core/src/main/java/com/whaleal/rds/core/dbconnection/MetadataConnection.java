package com.whaleal.rds.core.dbconnection;

import com.whaleal.rds.common.util.Log;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/30 5:38 下午
 */
public class MetadataConnection {
    private static Connection connection;
    private static final JdbcTemplate JDBC_TEMPLATE;

    /*
     * 初始化元数据链接  此信息应从配置文件中读取
     */
    static {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://lhp:3306/photon?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false&maxReconnects=10");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("lhp,,123");
        JDBC_TEMPLATE = new JdbcTemplate(basicDataSource);
        try {
            connection = basicDataSource.getConnection();
        } catch (SQLException e) {
            Log.error(  "链接元数据源出现异常,错误信息:" + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static JdbcTemplate getJdbcTemplate() {
        return JDBC_TEMPLATE;
    }

}
