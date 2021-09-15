package com.whaleal.photon.source.oracle;

import com.google.gson.Gson;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.datasource.DBUtil;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.util.Map;

public class GetDbTypeOfOracle {
    static Gson gson = new Gson();

    public static void main(String[] args) {
        Datasource proc4 = DBUtil.getSourceByProcName("proc10");
        OracleConnection.createConnection("proc10",proc4);
        Connection connection = OracleConnection.getConnection("proc10");
        JdbcTemplate jdbcTemplate = OracleConnection.getJdbcTemplate(proc4.getName());
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from COLUMNTYPE");
        map.entrySet().forEach(item -> {
            //System.out.println(item.getKey() + "-----" + item.getValue());
            //System.out.println(item.getValue().getClass());
            //System.out.println(item.getValue().getClass().getSimpleName().toUpperCase());
            String type = item.getValue().getClass().getSimpleName().toUpperCase();
            System.out.print(type + "(\"" + type + "\"),");
        });


//        while (iterator.hasNext()) {
//            Map.Entry<String, Object> next = iterator.next();
//            // System.out.println(next.getKey());
//            //System.out.println(next.getValue());
//            try {
//                // System.out.println(next.getValue().getClass());
//                String type = next.getValue().getClass().getSimpleName().toUpperCase();
//
//                System.out.print(type + "(\"" + type + "\"),");
//            } catch (Exception e) {
//
//            }
//
//        }
//
//
//        List list = new ArrayList();
//        list.add("1");
//        list.add(new Document().append("1",1).append("id",new ObjectId()));
//        String s = gson.toJson(list);
//        List list1=gson.fromJson(s.toString(),List.class);
//        System.out.println(s);
//        System.out.println(list1);
    }
}