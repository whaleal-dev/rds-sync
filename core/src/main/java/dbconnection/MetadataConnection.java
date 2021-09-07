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
        basicDataSource.setUrl(Property.getPropertiesByKey("url"));
        basicDataSource.setUsername(Property.getPropertiesByKey("userName"));
        basicDataSource.setPassword(Property.getPropertiesByKey("password"));
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
