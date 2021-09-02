package datasource;

import common.photonV.entity.Datasource;
import common.photonV.entity.ProgramInfo;
import programInfo.ProgramInfoUtil;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * 对公共数据库操作的基本类
 *
 * @author cs
 * @date 2021/08/30
 */
public class DBUtil {

    /**
     * 获取jdbc操作模版
     * 注意，这是我们主库的操作模版
     *
     * @return {@link JdbcTemplate}
     */
    public static JdbcTemplate getJdbcTemplate() {
        //完善存储程序的对象
        Datasource datasource = new Datasource();
        datasource.setUrl("jdbc:mysql://192.168.3.19:3306/photon?useUnicode=true&characterEncoding=utf-8&useSSL=false");
        datasource.setUsername("root");
        datasource.setPassword("123456");
        //根据连接信息获取连接
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(datasource.getUrl());
        basicDataSource.setUsername(datasource.getUsername());
        basicDataSource.setPassword(datasource.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(basicDataSource);
        return jdbcTemplate;
    }

    /**
     * 根据程序名获取到数据源
     *
     * @param procName proc的名字
     * @return {@link Datasource}
     */
    public static Datasource getSourceByProcName(String procName) {
        ProgramInfo confByProcName = ProgramInfoUtil.getProgramInfo(procName);
        String sourName = confByProcName.getSourceDsName();
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from photon.datasource where name='" + sourName + "' ");
        Datasource dataSource = new Datasource();
        dataSource.setId(map.get("id").toString());
        dataSource.setName(map.get("name").toString());
        dataSource.setType(map.get("type").toString());
        dataSource.setDsDatabase(map.get("ds_database").toString());
        dataSource.setUsername(map.get("username").toString());
        dataSource.setPassword(map.get("password").toString());
        dataSource.setId(map.get("ip").toString());
        dataSource.setUrl(map.get("url").toString());
        dataSource.setPort((String) map.get("port"));
        return dataSource;
    }

}
