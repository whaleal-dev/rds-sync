import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.columnclass.ColumnType;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.MetadataConnection;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import org.bson.Document;
import org.bson.types.Binary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
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
//        InputStream in = new FileInputStream("/Users/liheping/Desktop/photonT/execute/src/main/java/GetDbTypeOfMongodb.java");
//
//        byte[] bytes = new byte[]{};
//        try {
//            bytes = in.readAllBytes();
//            Connection connection = MetadataConnection.getConnection();
//
//            PreparedStatement statement = connection.prepareStatement("insert community.test(test)values(?)");
//
//
//            statement.setBytes(1,bytes);
//            statement.execute();
//
//        } catch (Exception e) {
//
//        }


//        OutputStream out = new FileOutputStream("/Users/liheping/Desktop/photonT/execute/src/main/java/GetDbTypeOfMongodb2.java");
//
//
//        Datasource datasource = DataSourceUtil.getDataSourceByDsName("mongodb2");
//        MongoDbConnection.createMonoDbClient("mongodb2", datasource);
//        MongoClient mongodb2 = MongoDbConnection.getMongoClient("mongodb2");
//
//        Document first = mongodb2.getDatabase("community").getCollection("test110").find().first();
//
//
//        Binary bytes = (Binary) first.get("test");
//        try {
//            out.write(bytes.getData());
//            out.close();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }


        Connection conn = MetadataConnection.getConnection();

        PreparedStatement pst = null;
        long beginTime = 0;
        long endTime = 0;
        try {
            if (conn != null) {
                System.out.println("获取连接成功");
                beginTime = System.currentTimeMillis();//开始计时
                String sqlPrefix = "insert into test2 (c1,c2,c3,c4,c5,c6,c7,c8,c9,z1,z2,z3,z4,z5,z6,z7,z8,z9,x10,x1,x2,x3) values ";
                // 保存sql后缀
                StringBuffer suffix = new StringBuffer();
                // 设置事务为非自动提交
                conn.setAutoCommit(false);
                // 比起st，pst会更好些
                pst = (PreparedStatement) conn.prepareStatement("");//准备执行语句
                // 外层循环，总提交事务次数
                for (int i = 1; i <= 100; i++) {
                    suffix = new StringBuffer();
                    // 第j次提交步长
                    for (int j = 1; j <= 10000; j++) {
                        // 构建SQL后缀
                        suffix.append("('" +
//                                i                                  +"','"+
                                UUID.randomUUID().toString() + "','" +
                                UUID.randomUUID().toString() + "','" +
                                i + "','" +
                                i + "','" +
                                j + "','" +
                                i * j + "','" +
                                (i / j) + "','" +
                                (i / j) + "','" +
                                (i / j) + "','" +
                                new Date(1)
                                + "','" +
                                new Time(1)
                                + "','" +
                                "1997" + "','" +
                                new Timestamp(1) + "','" +
                                new Timestamp(2) + "','" +
                                1 + "','" +
                                2 + "','" +
                                1 + "','" +
                                (i + j) + "','" +
                                1 + "','" +
                                1 + "','" +
                                1 + "','" +
                                (i + j) * j + "'" + "),");
                    }
                    // 构建完整SQL
                    //String sql = sqlPrefix + suffix.substring(0, suffix.length() - 1, suffix.length() - 2,suffix.length() - 3);
                    String sql = sqlPrefix + suffix.substring(0, suffix.length() - 1);
                    // 添加执行SQL
                    pst.addBatch(sql);
                    // 执行操作
                    pst.executeBatch();
                    // 提交事务
                    conn.commit();
                    // 清空上一次添加的数据
                    suffix = new StringBuffer();
                }
                endTime = System.currentTimeMillis();
                ;//开始计时
            } else {
                System.out.println("数据库连接失败");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("数据库地址错误");
        } finally {//释放资源
            System.out.println("插入成功，所有时间：" + (endTime - beginTime));
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pst != null) {
                try {
                    pst.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
