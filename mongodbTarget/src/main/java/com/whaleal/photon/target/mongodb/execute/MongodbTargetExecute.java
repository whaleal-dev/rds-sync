package com.whaleal.photon.target.mongodb.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractTargetExecute;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.target.mongodb.task.MongodbTargetTask;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.Set;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */

public class MongodbTargetExecute extends AbstractTargetExecute {

    public MongodbTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
    }

    @Override
    public void start() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), 1);
            TargetTaskPoolManager.submit(getProcNameAndBatchNo(), new MongodbTargetTask(programInfo, memoryCache));
        }
    }

    @Override
    public void deleteExistDbTable(Set<String> dbTableNameSet) {

        MongoClient mongoClient = MongoDbConnection.getMongoClient(getProcNameAndBatchNoAndTargetDsName());
        for (String dbTableName : dbTableNameSet) {
            try {
                String[] split = dbTableName.split("\\.", 2);
                String dbName = split[0];
                String tableName = split[1];
                mongoClient.getDatabase(dbName).getCollection(tableName).drop();
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void executePreSql(String sql) {

    }

    @Override
    public void rollBackDataFromDbTable(Set<String> dbTableNameSet) {
        for (String dbTableName : dbTableNameSet) {
            try {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        MongoClient mongoClient = MongoDbConnection.getMongoClient(getProcNameAndBatchNoAndTargetDsName());
                        for (int index = 0; index < 10; index++) {
                            String[] split = dbTableName.split("\\.", 2);
                            String dbName = split[0];
                            String tableName = split[1];
                            BasicDBObject condition = new BasicDBObject();
                            //回滚字段procNameAndBatchNo
                            condition.append("procNameAndBatchNo", getProcNameAndBatchNo() + "_" + index);
                            mongoClient.getDatabase(dbName).getCollection(tableName).deleteMany(condition);
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
}