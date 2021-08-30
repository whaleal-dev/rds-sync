package com.whaleal.photon.source.oracle;

import common.photonV.entity.Datasource;
import datasource.DBUtil;

import java.sql.Connection;

import static dbconnection.oracle.OracleConnection.getConnection;

public class ConnectionTest {
    public static void main(String[] args) {
        //getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        //Map<String, Object> map = OracleConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='mysql1' ");
        Datasource proc3 = DBUtil.getSourceByProcName("proc3");
        Connection connection = getConnection(proc3);
        System.out.println(connection);
        System.out.println(proc3.getDsDatabase());

    }
}
