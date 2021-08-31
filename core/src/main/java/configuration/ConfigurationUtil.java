package configuration;

import conf.Configuration;
import dbconnection.MetadataConnection;
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
        configuration.setProName(procName);
        Map<String, Object> map = MetadataConnection.getJdbcTemplate().queryForMap("select * from photon.program where proc_name='" + procName + "' ");

        Object taskName = map.get("task_name");
        if (taskName != null) {
            configuration.setTaskName(taskName.toString());
        }
        Object sourceDsName = map.get("source_ds_name");
        if (sourceDsName != null) {
            configuration.setSourceDsName(sourceDsName.toString());
        }
        Object targetDsName = map.get("target_ds_name");
        if (targetDsName != null) {
            configuration.setTargetDsName(targetDsName.toString());
        }
        Object dbTableWhite = map.get("db_table_white");
        if (dbTableWhite != null) {
            configuration.setDbTableWhite(dbTableWhite.toString());
        }
        Object syncMode = map.get("sync_mode");
        if (syncMode != null) {
            configuration.setSyncMode(syncMode.toString());
        }
        Object filterDdl = map.get("filter_ddl");
        if (filterDdl != null) {
            System.out.println(filterDdl);
            configuration.setFilterDdl(false);
            if ((Boolean) filterDdl ) {
                configuration.setFilterDdl(true);
            }
        }
        Object collectionExistDrop = map.get("collection_exist_drop");
        if (collectionExistDrop != null) {
            configuration.setCollectionExistDrop(false);
            if ((Boolean) collectionExistDrop) {
                configuration.setCollectionExistDrop(true);
            }
        }
        Object createIndex = map.get("create_index");
        if (createIndex != null) {
            configuration.setCreateIndex(false);
            if ((Boolean) createIndex) {
                configuration.setCreateIndex(true);
            }
        }
        Object targetThreadNum = map.get("target_thread_num");
        if (targetThreadNum != null) {
            configuration.setTargetThreadNum((Integer) targetThreadNum);
        } else {
            configuration.setTargetThreadNum(5);
        }
        Object sourceThreadNum = map.get("source_thread_num");
        if (sourceThreadNum != null) {
            configuration.setSourceThreadNum((Integer) targetThreadNum);
        } else {
            configuration.setSourceThreadNum(3);
        }
        Object cacheSize = map.get("cache_size");
        if (cacheSize != null) {
            configuration.setCacheSize((Integer) cacheSize);
        } else {
            configuration.setCacheSize(20);
        }
        Object cacheNum = map.get("cache_num");
        if (cacheNum != null) {
            configuration.setCacheNum((Integer) cacheNum);
        } else {
            configuration.setCacheNum(20);
        }
        Object dataBatchSize = map.get("data_batch_size");
        if (dataBatchSize != null) {
            configuration.setDataBatchSize((Integer) dataBatchSize);
        } else {
            configuration.setDataBatchSize(128);
        }
        Object syncParallel = map.get("sync_parallel");
        if (syncParallel != null) {
            configuration.setSyncParallel(false);
            if ((Boolean) syncParallel ) {
                configuration.setSyncParallel(true);
            }
        }
        Object startIncrementTime = map.get("start_increment_time");
        if (startIncrementTime != null) {
            configuration.setStartIncrementTime((Integer) startIncrementTime);
        } else {
            configuration.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        }
        Object incrementParseThreadNum = map.get("increment_parse_thread_num");
        if (incrementParseThreadNum != null) {
            configuration.setIncrementParseThreadNum((Integer) incrementParseThreadNum);
        } else {
            configuration.setIncrementParseThreadNum(5);
        }
        return configuration;
    }

}
