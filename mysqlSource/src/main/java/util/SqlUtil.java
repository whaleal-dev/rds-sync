package util;

import com.google.common.base.CaseFormat;
import org.bson.Document;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlUtil<T> {

    //获取连接池
    private Connection conn;

    public SqlUtil (Connection conn){
        this.conn = conn;
    }
    public SqlUtil (){
    }
    /**
     * 创建Statement实例
     *
     * @return
     */
    private  Statement getSt() {
        try {
            return conn.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    /**
     * 获取数据列表，返回list<Map>
     * @param sql
     * @param cla
     * @return
     */
    public List<Document> getAllMap(String sql, Class cla) {

        try {
            //获取数据库结果集的数据表
            ResultSet rs = this.getSt().executeQuery(sql);
            System.out.println("rs = " + rs);
            List<Document> datas = new ArrayList<>();
            while (rs.next()){
                Document doc = new Document(getADocument(rs, cla));
                datas.add(doc);
            }
            return datas;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    /**
     * 由ResultSet实例和指定的类型获取一个数据,返回map
     * @param rs
     * @param cla
     * @return
     */
    private Document getADocument(ResultSet rs, Class cla) {

        try {
            //获取有关ResultSet对象中列的类型和属性的信息的对象
            ResultSetMetaData md = rs.getMetaData();

            //map对象储存属性名与值
            Map<String, Object> map= new HashMap<>();
            Document document = new Document();

            if (rs != null) { //获取数据库内容不为空

//                System.out.println("edu.njxjc.manager.util.SqlUtil.getAData: 进入查询");
                for (int i = 1; i <= md.getColumnCount(); i++) { //遍历rs中的属性与值

                    //下划线改驼峰
                    String columnName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, md.getColumnName(i));
                    Object values =  rs.getObject(md.getColumnName(i));
//                    System.out.println("edu.njxjc.manager.util.SqlUtil.getAData:"+columnName+", " +values);

                    //写入属性名，值
                    map.put(columnName, values );

                    document.put(columnName, values);
                }
            }
            return document;

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

}