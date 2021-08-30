package configuration;

import common.photonV.entity.Datasource;
import conf.Configuration;
import datasource.DBUtil;
import dbconnection.mysql.MySqlConnection;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:24 下午
 */
public class ConfigurationUtil {

    /**
     * 根据程序的名字获取到整体的配置
     *
     * @param procName proc的名字
     * @return {@link Configuration}
     */
    public static Configuration getConfByProcName(String procName) {
        JdbcTemplate jdbcTemplate = DBUtil.getJdbcTemplate();
        Map<String, Object> objectMap = jdbcTemplate.queryForMap("select * from photon.program where proc_name='" + procName + "' ");
        Configuration configuration = new Configuration();
        configuration.setTaskName(objectMap.get("task_name").toString());
        configuration.setProName(objectMap.get("proc_name").toString());
        configuration.setSourceDsName(objectMap.get("source_ds_name").toString());
        configuration.setTargetDsName(objectMap.get("target_ds_name").toString());
        configuration.setSyncMode(objectMap.get("sync_mode").toString());
        configuration.setDbTableWhite(objectMap.get("db_table_white").toString());
        configuration.setFilterDdl(false);
        configuration.setCollectionExistDrop(true);
        configuration.setCreateIndex(true);
        configuration.setTargetThreadNum(5);
        configuration.setSourceThreadNum(2);
        configuration.setCacheNum(20);
        configuration.setCacheSize(20);
        configuration.setDataBatchSize(128);
        configuration.setSyncParallel(false);
        configuration.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        configuration.setIncrementParseThreadNum(5);
        return configuration;
    }

    public static Configuration getConfiguration(String procName) {
        Configuration configuration = new Configuration();
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.program where proc_name='" + procName + "' ");
        configuration.setTaskName("task1");
        configuration.setProName(map.get("proc_name").toString());
        configuration.setSourceDsName(map.get("source_ds_name").toString());
        configuration.setTargetDsName(map.get("target_ds_name").toString());
        configuration.setSyncMode("all");
        configuration.setDbTableWhite("photon.apply");
        configuration.setFilterDdl(false);
        configuration.setCollectionExistDrop(true);
        configuration.setCreateIndex(true);
        configuration.setTargetThreadNum(5);
        configuration.setSourceThreadNum(2);
        configuration.setCacheNum(20);
        configuration.setCacheSize(20);
        configuration.setDataBatchSize(128);
        configuration.setSyncParallel(false);
        configuration.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        configuration.setIncrementParseThreadNum(5);
        return configuration;
    }


}
