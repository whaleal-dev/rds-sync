package com.whaleal.photon.core.datasource;

import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.dbconnection.MetadataConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:19 下午
 */
public class DataSourceUtil {
    private static JdbcTemplate jdbcTemplate = MetadataConnection.getJdbcTemplate();

    public static Datasource getDataSourceByDsName(String dsName) {
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from photon.datasource where name='" + dsName + "' ");
        Datasource dataSource = new Datasource();
        System.out.println(map);
        dataSource.setName(dsName);
        Object type = map.get("type");
        if (type != null) {
            dataSource.setType(type.toString());
        }
        Object dsDatabase = map.get("ds_database");
        if (dsDatabase != null) {
            dataSource.setDsDatabase(dsDatabase.toString());
        }
        Object dsSchema = map.get("ds_schema");
        if (dsSchema != null) {
            dataSource.setDsSchema(dsSchema.toString());
        }
        Object userName = map.get("username");
        if (userName != null) {
            dataSource.setUsername(userName.toString());
        }
        Object password = map.get("password");
        if (password != null) {
            dataSource.setPassword(password.toString());
        }
        Object url = map.get("url");
        if (url != null) {
            dataSource.setUrl(url.toString());
        }
        Object ip = map.get("ip");
        if (ip != null) {
            dataSource.setId(ip.toString());
        }
        Object port = map.get("port");
        if (port != null) {
            dataSource.setPort(port.toString());
        }
        Object status = map.get("status");
        if (status != null) {
            dataSource.setStatus((Boolean) status);
        }

        Object remark = map.get("remark");
        if (remark != null) {
            dataSource.setRemark(remark.toString());
        }
        Object dsOption = map.get("ds_option");
        if (dsOption != null) {
            dataSource.setDsOption(dsOption.toString());
        }
        return dataSource;
    }
}
