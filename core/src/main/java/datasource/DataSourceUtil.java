package datasource;

import common.photonV.entity.Datasource;
import conf.Configuration;
import configuration.ConfigurationUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * 数据源的工具类
 * 根据程序的配置文件去查询到一个Datasource
 * 根据程序的名字获取到一个Datasource
 *
 * @author: lhp
 * @time: 2021/8/27 3:19 下午
 * @date 2021/08/30
 */
public class DataSourceUtil {


    public static Datasource getDataSourceByDsName(String procName, String dsName) {
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='" + dsName + "' ");
        Datasource dataSource = new Datasource();
        dataSource.setId(map.get("id").toString());
        dataSource.setName(map.get("name").toString());
        dataSource.setType(map.get("type").toString());
        dataSource.setDsDatabase(map.get("ds_database").toString());
        dataSource.setUsername(map.get("username").toString());
        dataSource.setPassword(map.get("password").toString());
        dataSource.setId(map.get("ip").toString());
        dataSource.setUrl(map.get("url").toString());
        dataSource.setPort((String) map.get("port"));
        return dataSource;
    }

    public static Datasource getDataSourceByDsName(String dsName) {
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='" + dsName + "' ");
        Datasource dataSource = new Datasource();
        dataSource.setId(map.get("id").toString());
        dataSource.setName(map.get("name").toString());
        dataSource.setType(map.get("type").toString());
        dataSource.setDsDatabase(map.get("ds_database").toString());
        dataSource.setUsername(map.get("username").toString());
        dataSource.setPassword(map.get("password").toString());
        dataSource.setId(map.get("ip").toString());
        dataSource.setUrl(map.get("url").toString());
        dataSource.setPort((String) map.get("port"));
        return dataSource;
    }

}
