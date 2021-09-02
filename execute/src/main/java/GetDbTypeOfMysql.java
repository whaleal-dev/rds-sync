import com.google.gson.Gson;
import common.column.AbstractColumn;
import dbconnection.MetadataConnection;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 5:06 下午
 */
public class GetDbTypeOfMysql {
    static Gson gson = new Gson();

    public static void main(String[] args) throws FileNotFoundException, SQLException {
//        InputStream in = new FileInputStream("/Users/liheping/Desktop/project/photonT/common/src/main/java/common/dbtype/DbTypeFlag.java");//生成被插入文件的节点流
//        Connection connection = MetadataConnection.getConnection();
//
//        PreparedStatement preparedStatement = connection.prepareStatement("insert into test.test(x2) values(?)");
//
//        preparedStatement.setBlob(1, in);
//        preparedStatement.executeUpdate();

        Connection connection = MetadataConnection.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from test.test");
        ResultSetMetaData md = resultSet.getMetaData();
        while (resultSet.next()) {
            List<AbstractColumn> abstractColumns = new ArrayList<>();
            //获取数据库内容不为空
            //遍历rs中的属性与值
            for (int i = 1; i <= md.getColumnCount(); i++) {
                //属性名下划线改驼峰
                String columnName = md.getColumnName(i);
                //值
                Object values = resultSet.getObject(md.getColumnName(i));

                if(values!=null){
                    String type = values.getClass().getSimpleName().toUpperCase();
                    System.out.print(type + "(\"" + type + "\"),");
                }



            }
        }

    }
}
