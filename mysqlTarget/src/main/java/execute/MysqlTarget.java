package execute;

import cache.MemoryCache;

import com.mongodb.client.MongoClient;
import common.photonV.entity.ProgramInfo;
import common.taskbase.AbstractTarget;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import task.MysqlTargetTask;
import thread.TargetTaskPoolManager;
import util.Log;

import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: MysqlTarget
 */

public class MysqlTarget extends AbstractTarget {

    public MysqlTarget(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        super(programInfo, memoryCache, procName);
    }

    @Override
    public void startToTarget() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 1);
            TargetTaskPoolManager.submit(procNameAndBatchNo, new MysqlTargetTask(programInfo, memoryCache));
        }
    }

    @Override
    public void deleteExistDbTable(Set<String> dbTableNameSet) {
        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName);
        for (String dbTableName : dbTableNameSet) {
            try {
                String sql = "drop table if EXISTS  `" + dbTableName + "`";
                System.out.println(sql);
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void rollBackDataFromDbTable(Set<String> dbTableNameSet) {
        for (String dbTableName : dbTableNameSet) {
            try {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName);
                        for (int index = 0; index < 10; index++) {
                            try {
                                String deleteDataSql = "delete from  " + dbTableName + " where  procNameAndBatchNo='" + procNameAndBatchNo + "_" + index + "'";
                                jdbcTemplate.execute(deleteDataSql);
                            } catch (Exception e) {
                                Log.error(e.getMessage());
                            }
                        }
                        TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, -1);
                    }
                };
                TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 1);
                TargetTaskPoolManager.submit(procNameAndBatchNo, runnable);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }


    public void createTable(Map<String, String> tableStructure) {
        Iterator<Map.Entry<String, String>> iterator = tableStructure.entrySet().iterator();

        while (iterator.hasNext()) {
            try {
                Map.Entry<String, String> next = iterator.next();
                JdbcTemplate jdbcTemplate = MySqlConnection.getJdbcTemplate(procNameAndBatchNoAndTargetDsName);
                String createSql = next.getValue();
                String dbName = next.getKey().split("\\.",2)[0];
                jdbcTemplate.execute("create database if not exists  "+dbName);
                jdbcTemplate.execute(createSql);
                Log.info(createSql);


            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e);
                Log.error(e.getMessage());
            }

        }
    }


}