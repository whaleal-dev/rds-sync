package programInfo;

import common.photonV.entity.ProgramInfo;
import dbconnection.MetadataConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:24 下午
 */
public class ProgramInfoUtil {
    private static JdbcTemplate jdbcTemplate = MetadataConnection.getJdbcTemplate();

    public static ProgramInfo getProgramInfo(String procName) {
        Map<String, Object> map = jdbcTemplate.queryForMap("select * from photon.program where proc_name='" + procName + "' ");
        ProgramInfo programInfo = new ProgramInfo();
        programInfo.setProName(procName);
        Object taskName = map.get("task_name");
        if (taskName != null) {
            programInfo.setTaskName(taskName.toString());
        }
        Object sourceDsName = map.get("source_ds_name");
        if (sourceDsName != null) {
            programInfo.setSourceDsName(sourceDsName.toString());
        }
        Object targetDsName = map.get("target_ds_name");
        if (targetDsName != null) {
            programInfo.setTargetDsName(targetDsName.toString());
        }
        Object dbTableWhite = map.get("db_table_white");
        if (dbTableWhite != null) {
            programInfo.setDbTableWhite(dbTableWhite.toString());
        }
        Object syncMode = map.get("sync_mode");
        if (syncMode != null) {
            programInfo.setSyncMode(syncMode.toString());
        }
        Object filterDdl = map.get("filter_ddl");
        if (filterDdl != null) {
            System.out.println(filterDdl);
            programInfo.setFilterDdl(false);
            if ((Boolean) filterDdl) {
                programInfo.setFilterDdl(true);
            }
        }
        Object collectionExistDrop = map.get("collection_exist_drop");
        if (collectionExistDrop != null) {
            programInfo.setCollectionExistDrop(false);
            if ((Boolean) collectionExistDrop) {
                programInfo.setCollectionExistDrop(true);
            }
        }
        Object createIndex = map.get("create_index");
        if (createIndex != null) {
            programInfo.setCreateIndex(false);
            if ((Boolean) createIndex) {
                programInfo.setCreateIndex(true);
            }
        }
        Object targetThreadNum = map.get("target_thread_num");
        if (targetThreadNum != null) {
            programInfo.setTargetThreadNum((Integer) targetThreadNum);
        } else {
            programInfo.setTargetThreadNum(5);
        }
        Object sourceThreadNum = map.get("source_thread_num");
        if (sourceThreadNum != null) {
            programInfo.setSourceThreadNum((Integer) targetThreadNum);
        } else {
            programInfo.setSourceThreadNum(3);
        }
        Object cacheSize = map.get("cache_size");
        if (cacheSize != null) {
            programInfo.setCacheSize((Integer) cacheSize);
        } else {
            programInfo.setCacheSize(20);
        }
        Object cacheNum = map.get("cache_num");
        if (cacheNum != null) {
            programInfo.setCacheNum((Integer) cacheNum);
        } else {
            programInfo.setCacheNum(20);
        }
        Object dataBatchSize = map.get("data_batch_size");
        if (dataBatchSize != null) {
            programInfo.setDataBatchSize((Integer) dataBatchSize);
        } else {
            programInfo.setDataBatchSize(128);
        }
        Object syncParallel = map.get("sync_parallel");
        if (syncParallel != null) {
            programInfo.setSyncParallel(false);
            if ((Boolean) syncParallel) {
                programInfo.setSyncParallel(true);
            }
        }
        Object startIncrementTime = map.get("start_increment_time");
        if (startIncrementTime != null) {
            programInfo.setStartIncrementTime((Integer) startIncrementTime);
        } else {
            programInfo.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        }
        Object incrementParseThreadNum = map.get("increment_parse_thread_num");
        if (incrementParseThreadNum != null) {
            programInfo.setIncrementParseThreadNum((Integer) incrementParseThreadNum);
        } else {
            programInfo.setIncrementParseThreadNum(5);
        }
        return programInfo;
    }

}
