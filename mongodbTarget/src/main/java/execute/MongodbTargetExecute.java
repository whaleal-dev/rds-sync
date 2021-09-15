package execute;

import cache.MemoryCache;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import common.photonV.entity.ProgramInfo;
import common.taskbase.AbstractTargetExecute;
import dbconnection.mongodb.MongoDbConnection;
import task.MongodbTargetTask;
import thread.TargetTaskPoolManager;
import util.Log;

import java.util.Set;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */

public class MongodbTargetExecute extends AbstractTargetExecute {
    public MongodbTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        super(programInfo, memoryCache, procName);
    }

    @Override
    public void startToTarget() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 1);
            TargetTaskPoolManager.submit(procNameAndBatchNo, new MongodbTargetTask(programInfo, memoryCache));
        }
    }

    @Override
    public void deleteExistDbTable(Set<String> dbTableNameSet) {

        String syncMode = programInfo.getSyncMode();

        MongoClient mongoClient = MongoDbConnection.getMongoClient(procNameAndBatchNoAndTargetDsName);
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
    public void rollBackDataFromDbTable(Set<String> dbTableNameSet) {
        for (String dbTableName : dbTableNameSet) {
            try {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        MongoClient mongoClient = MongoDbConnection.getMongoClient(procNameAndBatchNoAndTargetDsName);
                        for (int index = 0; index < 10; index++) {
                            String[] split = dbTableName.split("\\.", 2);
                            String dbName = split[0];
                            String tableName = split[1];
                            BasicDBObject condition = new BasicDBObject();
                            condition.append("procNameAndBatchNo", procNameAndBatchNo + "_" + index);
                            mongoClient.getDatabase(dbName).getCollection(tableName).deleteMany(condition);
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
}