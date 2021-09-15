import com.google.gson.Gson;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.MetadataConnection;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;

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
public class GetDbTypeOfOracle {
    static Gson gson = new Gson();

    public static void main(String[] args) throws FileNotFoundException, SQLException {
//        InputStream in = new FileInputStream("/Users/liheping/Desktop/project/photonT/com.whaleal.photon.common.common/src/main/java/com.whaleal.photon.common.common/dbtype/DbTypeFlag.java");//生成被插入文件的节点流
//        Connection connection = MetadataConnection.getConnection();
//
//        PreparedStatement preparedStatement = connection.prepareStatement("insert into test.test(x2) values(?)");
//
//        preparedStatement.setBlob(1, in);
//        preparedStatement.executeUpdate();
//
        Datasource pg = DataSourceUtil.getDataSourceByDsName("oracle3");

        OracleConnection.createConnection(pg.getName(), pg);
        Connection pgConnection = PgServerConnection.getConnection("oracle3");
        Statement statement = OracleConnection.getConnection("oracle3").createStatement();
        ResultSet resultSet = statement.executeQuery("select * from TTYPE");
        ResultSetMetaData md = resultSet.getMetaData();
        System.out.println(System.currentTimeMillis());
        int num=0;
        while (resultSet.next()) {
            List<AbstractColumn> abstractColumns = new ArrayList<>();
            //获取数据库内容不为空
            //遍历rs中的属性与值\
            if(num++>100000){
                break;
            }

            System.out.println(num);
//            for (int i = 1; i <= md.getColumnCount(); i++) {
//                //属性名下划线改驼峰
//                String columnName = md.getColumnName(i);
//                //值
//                Object values = resultSet.getObject(md.getColumnName(i));
//
//            }
        }
        System.out.println(System.currentTimeMillis());




    }
}
