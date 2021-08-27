package main;

import conf.Configuration;
import conf.ReaderSplitUtil;
import constant.Key;

import java.io.File;
import java.util.List;

/**
 * mysql测试
 *
 * @author: jy
 * @Date: 2021/08/24
 */
public class Mysql2MongoDb {
    public static void main(String[] args) {
        //配置文件
        File file = new File("/Users/cs/Documents/3.json");
        Configuration con = Configuration.from(file);
        System.out.println("con = " + con);
//        String a = DataBaseType.parseIpFromJdbcUrl("jdbc:mysql://192.168.3.106:3306/community?characterEncoding=utf-8&useSSL=false");
//        System.out.println(a);
        List<Configuration> list = ReaderSplitUtil.doSplit(con, 2);
        for (Configuration configuration : list) {
            System.out.println(configuration);
            System.out.println(configuration.getString(Key.QUERY_SQL));
            System.out.println(configuration.getString("target.jdbcUrl"));
        }
        System.out.println("list = " + list);

    }

}
