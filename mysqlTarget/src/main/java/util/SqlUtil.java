package util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlUtil {

    //获取连接池
    private static Connection conn = DbcpUtil.getConnection();

    private static Statement st = getSt();

    /**
     * 创建Statement实例
     *
     * @return
     */
    private static Statement getSt() {
        try {
            return conn.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }




    /**
     * 执行数据库语句
     *
     * @param sql
     * @return
     */
    public static void data(String sql) {
        //SQL语句执行检测
        try {
            st.executeUpdate(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    /**
     * 查询数据库中是否存在数据
     *
     * @param sql
     * @return int
     */
    public static int isData(String sql) {

        ResultSet rs = null;
        int count=0;

        try {
            rs = st.executeQuery(sql);
            if(rs.next()){
                count=rs.getInt(1);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return count;


    }

    /**
     * 获取数据列表，返回list
     * @param sql
     * @return
     */
    public static List<Map<String,Object>> selectData(String sql) {

        try {
            //获取数据库结果集中的数据
            ResultSet rs = st.executeQuery(sql);

            List<Map<String,Object>> data1 = new ArrayList<>();

            while (rs.next()){
                Map map = new HashMap();
                map.put("column_name",rs.getString("column_name"));
                map.put("data_type",rs.getString("data_type"));
                //字符长度
                map.put("CHARACTER_MAXIMUM_LENGTH",rs.getString("CHARACTER_MAXIMUM_LENGTH"));
                //小数位
                map.put("NUMERIC_SCALE",rs.getString("NUMERIC_SCALE"));


                data1.add(map);
            }
            return data1;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

}