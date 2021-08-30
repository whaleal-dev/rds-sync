package datasource;

import common.photonV.entity.Datasource;
import dbconnection.mysql.MySqlConnection;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:19 下午
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
