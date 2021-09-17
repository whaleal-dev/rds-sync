package com.whaleal.photon.target.mysql.execute;

import com.whaleal.photon.common.cache.MemoryCache;

import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractTargetExecute;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.target.mysql.task.MysqlTargetTask;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: MysqlTargetExecute
 */

public class MysqlTargetExecute extends AbstractTargetExecute {

    public MysqlTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
    }

    @Override
    public void start() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), 1);
            TargetTaskPoolManager.submit(getProcNameAndBatchNo(), new MysqlTargetTask(programInfo, memoryCache));
        }
    }

    @Override
    public void deleteExistDbTable(Set<String> dbTableNameSet) {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndTargetDsName());
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
    public void executePreSql(String sql) {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndTargetDsName());
        try {
            jdbcTemplate.execute(sql);
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
                        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndTargetDsName());
                        for (int index = 0; index < 10; index++) {
                            try {
                                String deleteDataSql = "delete from  " + dbTableName + " where  procNameAndBatchNo='" + getProcNameAndBatchNo() + "_" + index + "'";
                                jdbcTemplate.execute(deleteDataSql);
                            } catch (Exception e) {
                                Log.error(e.getMessage());
                            }
                        }
                        TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), -1);
                    }
                };
                TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), 1);
                TargetTaskPoolManager.submit(getProcNameAndBatchNo(), runnable);
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
                JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(getProcNameAndBatchNoAndTargetDsName());
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