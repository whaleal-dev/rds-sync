package com.whaleal.rds.core.programInfo;

import com.whaleal.rds.common.common.syncerV.entity.ProgramInfo;
import com.whaleal.rds.core.dbconnection.MetadataConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/27 3:24 下午
 */
public class ProgramInfoUtil {
    private static final JdbcTemplate JDBC_TEMPLATE = MetadataConnection.getJdbcTemplate();

    public static ProgramInfo getProgramInfo(String procName) {
        ProgramInfo programInfo = null;
        try {
            Map<String, Object> map = JDBC_TEMPLATE.queryForMap("select * from photon.program where proc_name='" + procName + "' ");
            programInfo = new ProgramInfo();
            programInfo.setProName(procName);
            Object taskName = map.get("task_name");
            if (taskName != null) {
                programInfo.setTaskName(taskName.toString());
            }
            Object sourceDsName = map.get("source_ds_name");
            if (sourceDsName != null) {
                programInfo.setSourceDsName(sourceDsName.toString());
            }
            Object sinkDsName = map.get("sink_ds_name");
            if (sinkDsName != null) {
                programInfo.setSinkDsName(sinkDsName.toString());
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
                programInfo.setAutoDropExistDbTable(false);
                if ((Boolean) collectionExistDrop) {
                    programInfo.setAutoDropExistDbTable(true);
                }
            }
            Object createIndex = map.get("create_index");
            if (createIndex != null) {
                programInfo.setAutoCreateIndex(false);
                if ((Boolean) createIndex) {
                    programInfo.setAutoCreateIndex(true);
                }
            }
            Object sinkThreadNum = map.get("sink_thread_num");
            if (sinkThreadNum != null) {
                programInfo.setSinkThreadNum((Integer) sinkThreadNum);
            } else {
                programInfo.setSinkThreadNum(5);
            }
            Object sourceThreadNum = map.get("source_thread_num");
            if (sourceThreadNum != null) {
                programInfo.setSourceThreadNum((Integer) sourceThreadNum);
            } else {
                programInfo.setSourceThreadNum(3);
            }
            Object cacheSize = map.get("cache_size");
            if (cacheSize != null) {
                programInfo.setCacheBucketSize((Integer) cacheSize);
            } else {
                programInfo.setCacheBucketSize(20);
            }
            Object cacheNum = map.get("cache_num");
            if (cacheNum != null) {
                programInfo.setCacheBucketNum((Integer) cacheNum);
            } else {
                programInfo.setCacheBucketNum(20);
            }
            Object dataBatchSize = map.get("data_batch_size");
            if (dataBatchSize != null) {
                programInfo.setDataBatchSize((Integer) dataBatchSize);
            } else {
                programInfo.setDataBatchSize(128);
            }

            Object startIncrementTime = map.get("start_increment_time");
            if (startIncrementTime != null) {
                programInfo.setStartIncrementTime((Integer) startIncrementTime);
            } else {
                programInfo.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
            }
            Object incrementParseThreadNum = map.get("increment_parse_thread_num");
            if (incrementParseThreadNum != null) {
                programInfo.setRealTimeThreadNum((Integer) incrementParseThreadNum);
            } else {
                programInfo.setRealTimeThreadNum(5);
            }

        } catch (Exception e) {

        }
        return programInfo;
    }
}
