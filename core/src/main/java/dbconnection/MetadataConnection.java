package dbconnection;

import conf.Property;
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
    private static JdbcTemplate jdbcTemplate;

    /*
     * 初始化元数据链接  此信息应从配置文件中读取
     */
    static {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:mysql://192.168.3.19:3306/photon?useUnicode=true&characterEncoding=utf-8");
        basicDataSource.setUsername("root");
        basicDataSource.setPassword("123456");
        jdbcTemplate = new JdbcTemplate(basicDataSource);
        try {
            connection = basicDataSource.getConnection();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
