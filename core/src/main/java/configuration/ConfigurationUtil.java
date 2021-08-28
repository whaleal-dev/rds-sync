package configuration;

import conf.Configuration;
import dbconnection.mysql.MySqlConnection;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:24 下午
 */
public class ConfigurationUtil {
    public static Configuration getConfiguration(String procName) {
        Configuration configuration = new Configuration();
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.program where proc_name='" + procName + "' ");
        configuration.setProName(map.get("proc_name").toString());
        configuration.setSourceDsName(map.get("source_ds_name").toString());
        configuration.setTargetDsName(map.get("target_ds_name").toString());
        configuration.setSyncMode("all");
        configuration.setDbTableWhite("photon.+");
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
