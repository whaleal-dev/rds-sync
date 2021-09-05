import com.google.gson.Gson;
import common.column.AbstractColumn;
import common.photonV.entity.Datasource;
import datasource.DataSourceUtil;
import dbconnection.MetadataConnection;
import dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileNotFoundException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 5:06 下午
 */
public class GetDbTypeOfPG {
    static Gson gson = new Gson();

    public static void main(String[] args) throws FileNotFoundException, SQLException {
//        InputStream in = new FileInputStream("/Users/liheping/Desktop/project/photonT/common/src/main/java/common/dbtype/DbTypeFlag.java");//生成被插入文件的节点流
//        Connection connection = MetadataConnection.getConnection();
//
//        PreparedStatement preparedStatement = connection.prepareStatement("insert into test.test(x2) values(?)");
//
//        preparedStatement.setBlob(1, in);
//        preparedStatement.executeUpdate();
//
        Datasource pg = DataSourceUtil.getDataSourceByDsName("pg");

//        PgServerConnection.createConnection(pg.getName(), pg);
//        Connection pgConnection = PgServerConnection.getConnection("pg");
//        Statement statement = pgConnection.createStatement();
//        System.out.println(3);
//        ResultSet resultSet = statement.executeQuery("select * from nettb");
//        statement.setFetchSize(20);
//        statement.setQueryTimeout(10);
//        System.out.println(1);
//        ResultSetMetaData md = resultSet.getMetaData();
//        System.out.println(2);
//        while (resultSet.next()) {
//            System.out.println(  " =====    "  );
//            List<AbstractColumn> abstractColumns = new ArrayList<>();
//            //获取数据库内容不为空
//            //遍历rs中的属性与值
//            for (int i = 1; i <= md.getColumnCount(); i++) {
//                //属性名下划线改驼峰
//                String columnName = md.getColumnName(i);
//                //值
//                Object values = resultSet.getObject(md.getColumnName(i));
//
////                if (values != null) {
////                    String type = values.getClass().getSimpleName().toUpperCase();
////                    System.out.print(type + "(\"" + type + "\"),");
////                }
//                System.out.println(columnName + "     " + values);
//
//            }
//        }


        PgServerConnection.createConnection(pg.getName(), pg);
        Connection pgConnection = PgServerConnection.getConnection("pg");
        pgConnection.setAutoCommit(false);
        Statement statement = pgConnection.createStatement();
        PreparedStatement ps = pgConnection.prepareStatement("select * from nettb",
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

       //也可以修改jdbc url通过defaultFetchSize参数来设置，这样默认所以的返回结果都是通过流方式读取.
        ps.setFetchSize(200);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            System.out.println(rs.getString("id"));
        }


    }
}
