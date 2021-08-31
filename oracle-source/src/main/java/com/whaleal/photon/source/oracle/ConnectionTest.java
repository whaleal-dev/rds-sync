package com.whaleal.photon.source.oracle;

import common.photonV.entity.Datasource;
import datasource.DBUtil;
import dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.Map;

import static dbconnection.oracle.OracleConnection.getConnection;

public class ConnectionTest {
    public static void main(String[] args) {
        //getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        //Map<String, Object> map = OracleConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='mysql1' ");
        Datasource proc4 = DBUtil.getSourceByProcName("proc4");
        Connection connection = OracleConnection.createConnection(proc4);
        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplateBySource(proc4);
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from CS.test");
        map.entrySet().forEach(item -> {
            System.out.println(item.getKey() + "-----" + item.getValue());
        });
        System.out.println(connection);
        System.out.println(proc4.getDsDatabase());
    }
}
