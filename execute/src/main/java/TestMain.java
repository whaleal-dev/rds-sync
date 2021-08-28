import cache.MemoryCache;
import common.photonV.entity.Datasource;
import conf.Configuration;

import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import execute.MongodbSource;
import execute.MongodbTarget;
import execute.MysqlTargetLhp;
import task.MongodbTargetTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import util.Log;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMain {
    public static void main(String[] args) {

        testMongoDbToMongoDb();
        testMongoDbToMongoDb();
        testMongoDbToMongoDb();
    }


    public static void testMongoDbToMongoDb() {

        Configuration configuration = ConfigurationUtil.getConfiguration("proc1");

        MongoDbConnection.getMongoClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getSourceDsName()));

        MongoDbConnection.getMongoClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getTargetDsName()));

        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(),
                configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);


        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(), configuration.getSourceThreadNum(), configuration.getSourceThreadNum());
        SourceTaskPoolManager.addSourceTaskPoolManager(configuration.getProName(), sourceTaskPoolManager);

        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 2, 3);
        SysPoolManager.addSysTaskPoolManager(configuration.getProName(), sysPoolManager);

        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(configuration.getProName(), 1,
                1);
        TargetTaskPoolManager.addTargetTaskPoolManager(configuration.getProName(), targetTaskPoolManager);

        MongodbTarget mongodbTarget = new MongodbTarget(configuration, memoryCache, configuration.getProName());
        mongodbTarget.startToTarget();

        MongodbSource mongodbSource = new MongodbSource(configuration, memoryCache);
        mongodbSource.createTask();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(configuration.getProName(), 0);
                boolean getAllDbTable = mongodbSource.isGetAllDbTable();
                int sourceTaskQueueSize = mongodbSource.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(configuration.getProName(), 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(configuration.getProName(), 0);
                int allDataCacheNum = memoryCache.getAllDataCacheNum();
                Log.info(setSysActiveThreadNum + "");
                Log.info("sum:" + (sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum));
                Log.info("sourceThread:" + sourceThread);
                Log.info("sourceTaskQueueSize:" + sourceTaskQueueSize);
                Log.info("setSysActiveThreadNum:" + setSysActiveThreadNum);
                Log.info("targetActiveThreadNum:" + targetActiveThreadNum);
                Log.info("allDataCacheNum:" + allDataCacheNum);
                Log.info("getAllDbTable:" + getAllDbTable);
                if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
                    Thread.sleep(10000);
                    try {

                        TargetTaskPoolManager.shuntDownNow(configuration.getProName());

                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.shuntDownNow(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.shuntDownNow(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    memoryCache.gcMemoryCache();
                    MongodbTargetTask.setIsStopFlagOfTarget(configuration.getProName(), false);
//                    MongoDbConnection.close(configuration.getTargetName());
//                    MongoDbConnection.close(configuration.getSourceName());
                    //MySqlConnection.close("1");
                    Log.info("procName:" + configuration.getProName() + "关闭成功");
                    Thread.sleep(10000);
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(configuration.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

}
