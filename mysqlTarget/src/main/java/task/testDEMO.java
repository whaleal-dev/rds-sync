package task;

import org.bson.Document;
import util.SqlUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: zb
 * @time: 2021/8/19 10:48 上午
 * @desc: 写入数据
 */

public class testDEMO {
    public static void main(String[] args) {
        Map<String,String> hashMap = new HashMap<>();
        Document document=new Document();
        document.put("c1",1);
        document.put("c2","str2");
        document.put("c3",new Double(1));
        document.put("c4",new Date());
        document.put("c5",2);
        document.put("c6","asd");
        hashMap.put("c1","Integer");
        hashMap.put("c2","String");
        hashMap.put("c3","Double");
        hashMap.put("c4","Stirng");


        System.out.println(document);
        Iterator<Map.Entry<String, Object>> iterator = document.entrySet().iterator();
        //字段
        StringBuffer column = new StringBuffer();
        //属性值
        StringBuffer value = new StringBuffer();
        while (iterator.hasNext()){
            Map.Entry<String, Object> next = iterator.next();
            String type = next.getValue().getClass().getSimpleName();
            int length = next.getValue().toString().length();

            //查询数据库中是否存在这个表
            int num = SqlUtil.isData("select count(*)  from information_schema.TABLES t where t.TABLE_SCHEMA ='photon' and t.TABLE_NAME ='test'");
            if(num == 1){
                //存在，查询表中的结构
                List<Map<String,Object>> array = new ArrayList<Map<String,Object>>();
                //查询表中的字段和类型
                String sql = "select column_name,data_type from information_schema.COLUMNS where table_name = 'test' and table_schema = 'photon'";
                array = SqlUtil.selectData(sql);
                for (int i = 0;i < array.size();i++) {
                    if(!hashMap.containsKey(array.get(i).get("column_name"))){
                        hashMap.put(array.get(i).get("column_name").toString(),array.get(i).get("data_type").toString()+"(255))");
                    }
                }
            } else {
                //不存在，创建表
                hashMap.put(next.getKey(),next.getValue().toString());
                SqlUtil.data("CREATE Table test ("+ next.getKey() +" " + type +"(255))");
            }
            //HashMap中没有这个属性就加入
            if (!hashMap.containsKey(next.getKey())){
                hashMap.put(next.getKey(),next.getValue().toString());
                String sql;
                if (type.equals("String")||type.equals("Date")||type.equals("Map")||type.equals("Array")||type.equals("Boolean")){
                    sql = "ALTER TABLE test ADD " + next.getKey() +" "+ "varchar(255)";
                } else if(type.equals("Double")||type.equals("Long")||type.equals("Float")){
                    sql = "ALTER TABLE test ADD " + next.getKey() +" " + type;
                } else {
                    sql = "ALTER TABLE test ADD " + next.getKey() +" " + type +"(" + length +")";
                }
                //表中加入这个字段
                System.out.println(sql);
                SqlUtil.data(sql);
            }
            column.append(next.getKey()).append(",");
            System.out.println(next.getValue().toString().length());
            if (type.equals("String")||type.equals("Date")||type.equals("Map")||type.equals("Array")||type.equals("Boolean")){
                value.append("'").append(next.getValue()).append("'").append(",");
            } else {
                value.append(next.getValue()).append(",");
            }
            System.out.println(next.getValue().getClass().getSimpleName());
        }
        String sql = "insert into test (" + column.substring(0,column.length()-1)
                + ") values (" + value.substring(0,value.length()-1) + ")";

        System.out.println(sql);
        SqlUtil.data(sql);
    }
}
