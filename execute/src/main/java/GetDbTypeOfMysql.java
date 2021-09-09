import com.google.gson.Gson;
import common.column.AbstractColumn;
import common.photonV.entity.Datasource;
import datasource.DataSourceUtil;
import dbconnection.MetadataConnection;
import dbconnection.mysql.MySqlConnection;
import dbconnection.pgserver.PgServerConnection;

import java.io.FileNotFoundException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Date;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 5:06 下午
 */
public class GetDbTypeOfMysql {
    static Gson gson = new Gson();

    public static void main(String[] args) throws FileNotFoundException, SQLException, InterruptedException {
//        InputStream in = new FileInputStream("/Users/liheping/Desktop/project/photonT/common/src/main/java/common/dbtype/DbTypeFlag.java");//生成被插入文件的节点流
//
//        Connection connection = MetadataConnection.getConnection();
//        Statement statement = connection.createStatement();
//        ResultSet resultSet = statement.executeQuery("select * from test.test");
//        ResultSetMetaData md = resultSet.getMetaData();
//        while (resultSet.next()) {
//            List<AbstractColumn> abstractColumns = new ArrayList<>();
//            //获取数据库内容不为空
//            //遍历rs中的属性与值
//            for (int i = 1; i <= md.getColumnCount(); i++) {
//                //属性名下划线改驼峰
//                String columnName = md.getColumnName(i);
//                //值
//                Object values = resultSet.getObject(md.getColumnName(i));
//                if(values!=null){
//                    String type = values.getClass().getSimpleName().toUpperCase();
//                     System.out.print(type + "(\"" + type + "\"),");
//                   // System.out.println(columnName+"       "+values);
//                }
//
//            }
//        }


//        Date date=new java.util.Date(1630553229060L);
//        System.out.println(date.toString());
//        Instant instant = date.toInstant();
//        ZoneId zoneId = ZoneId.systemDefault();
//        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
//        System.out.println("Date = " + date);
//        System.out.println("LocalDateTime = " + localDateTime.toString());



        Datasource mysqltest = DataSourceUtil.getDataSourceByDsName("mysqltest");

        MySqlConnection.createConnection(mysqltest.getName(), mysqltest);
        Connection pgConnection = MySqlConnection.getConnection("mysqltest");
        Statement statement = pgConnection.createStatement();
        PreparedStatement ps = pgConnection.prepareStatement("select * from blog.ms_article");

        ps.setFetchSize(128);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            System.out.println(rs.getString(1));
        }


    }
}
