package com.whaleal.rds.sink.mysql.execute;

import com.whaleal.rds.common.cache.MemoryCache;

import com.whaleal.rds.common.common.syncerV.entity.ProgramInfo;
import com.whaleal.rds.common.common.taskbase.AbstractSinkExecute;
import com.whaleal.rds.core.dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.rds.sink.mysql.task.MysqlSinkTask;
import com.whaleal.rds.core.thread.SinkTaskPoolManager;
import com.whaleal.rds.common.util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: MysqlSinkExecute
 */

public class MysqlSinkExecute extends AbstractSinkExecute {

    public MysqlSinkExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
    }

    @Override
    public void start() {
        for (int i = 0; i < programInfo.getSinkThreadNum(); i++) {
            SinkTaskPoolManager.setSinkActiveThreadNum(getProcNameAndBatchNo(), 1);
            SinkTaskPoolManager.submit(getProcNameAndBatchNo(), new MysqlSinkTask(programInfo, memoryCache));
        }
    }

    @Override
    public void dropExistDbTable(Set<String> dbTableNameSet) {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndSinkDsName());
        for (String dbTableName : dbTableNameSet) {
            try {
                String sql = "drop table if EXISTS  `" + dbTableName + "`";
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void preExecute(Object sql) {
        if (sql == null) {
            return;
        }
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndSinkDsName());
        try {
            jdbcTemplate.execute(String.valueOf(sql));
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    @Override
    public void rollBackDataFromDbTable(Set<String> dbTableNameSet) {
        for (String dbTableName : dbTableNameSet) {
            try {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndSinkDsName());
                        for (int index = 0; index < 10; index++) {
                            try {
                                String deleteDataSql = "delete from  " + dbTableName + " where  procNameAndBatchNo='" + getProcNameAndBatchNo() + "_" + index + "'";
                                jdbcTemplate.execute(deleteDataSql);
                            } catch (Exception e) {
                                Log.error(e.getMessage());
                            }
                        }
                        SinkTaskPoolManager.setSinkActiveThreadNum(getProcNameAndBatchNo(), -1);
                    }
                };
                SinkTaskPoolManager.setSinkActiveThreadNum(getProcNameAndBatchNo(), 1);
                SinkTaskPoolManager.submit(getProcNameAndBatchNo(), runnable);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    /**
     * 建表
     *
     * @param tableStructure
     */
    public void createTable(Map<String, String> tableStructure) {
        Iterator<Map.Entry<String, String>> iterator = tableStructure.entrySet().iterator();
        while (iterator.hasNext()) {
            try {
                Map.Entry<String, String> next = iterator.next();
                JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndSinkDsName());
                String createSql = next.getValue();
                String dbName = next.getKey().split("\\.", 2)[0];
                jdbcTemplate.execute("create database if not exists  " + dbName);
                jdbcTemplate.execute(createSql);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                Log.error(e.getMessage());
            }
        }
    }
}
