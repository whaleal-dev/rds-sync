package com.whaleal.photon.source.oracle;

import common.photonV.entity.Datasource;
import datasource.DBUtil;

import dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class ConnectionTest {

    public static void main(String[] args) {

        Datasource proc4 = DBUtil.getSourceByProcName("proc4");
        OracleConnection.createConnection("proc4",proc4);
        Connection connection = OracleConnection.getConnection("proc4");
        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplate(proc4.getName());
        List<Map<String, Object>> dbTableList = jdbcTemplate.queryForList("select * from USER_TABLES");
        for (Map dbTableMap : dbTableList) {
            String dbName = dbTableMap.get("TABLESPACE_NAME").toString();
            String tableName = dbTableMap.get("TABLE_NAME").toString();
            System.out.println(tableName);
        }
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from STU");
        map.entrySet().forEach(item -> {
            System.out.println(item.getKey() + "-----" + item.getValue());
        });
    }
}
