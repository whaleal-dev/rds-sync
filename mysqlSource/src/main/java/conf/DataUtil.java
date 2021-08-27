package conf;

import com.google.common.base.CaseFormat;
import common.column.AbstractColumn;
import parse.TransformationMysqlDataToColumn;

import java.sql.*;
import java.util.*;

/**
 * Mysql数据工具类
 *
 * @author: jy
 * @Date: 2021/08/27
 */
public class DataUtil {

    //获取连接池
    private Connection conn;

    public DataUtil (Connection conn){
        this.conn = conn;
    }

    public DataUtil (){
    }

    /**
     * 创建Statement实例
     *
     * @return
     */
    private Statement getSt() {
        try {
            return conn.createStatement();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    /**
     * 获取datalist
     *
     * @param sql 切分的 sql 语句
     * @return
     */
    public List<List<AbstractColumn>> getMysqlDatalist(String sql) {
        try {
            //获取数据库结果集的数据表
            ResultSet rs = this.getSt().executeQuery(sql);
            List<List<AbstractColumn>> dataList = new ArrayList<>();
            while (rs.next()){
                dataList.add(getMysqlAbstractColumn(rs));
            }
            return dataList;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    /**
     * 获取AbstractColumn集合
     *
     * @param rs
     * @return
     */
    private List<AbstractColumn> getMysqlAbstractColumn(ResultSet rs) {
        try {
            //获取有关ResultSet对象中列的类型和属性的信息的对象
            ResultSetMetaData md = rs.getMetaData();
            Map<String, Object> map= new HashMap<>();
            List<AbstractColumn> abstractColumns = new ArrayList<>();
            //获取数据库内容不为空
            if (rs != null) {
                //遍历rs中的属性与值
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    //属性名下划线改驼峰
                    String columnName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, md.getColumnName(i));
                    //值
                    Object values =  rs.getObject(md.getColumnName(i));
                    //写入属性名，值
                    map.put(columnName, values );
                }
            }
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> next = iterator.next();
                AbstractColumn abstractColumn = TransformationMysqlDataToColumn.parseValue(next.getKey(), next.getValue());
                abstractColumns.add(abstractColumn);
            }
            return abstractColumns;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

}
