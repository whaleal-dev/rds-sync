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
                System.out.println(setSysActiveThreadNum);
                System.out.println("sum:" + (sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum));
                System.out.println("sourceThread:" + sourceThread);
                System.out.println("sourceTaskQueueSize:" + sourceTaskQueueSize);
                System.out.println("setSysActiveThreadNum:" + setSysActiveThreadNum);
                System.out.println("targetActiveThreadNum:" + targetActiveThreadNum);
                System.out.println("allDataCacheNum:" + allDataCacheNum);
                System.out.println("getAllDbTable:" + getAllDbTable);
                if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
                    Thread.sleep(10000);
                    try {
                        SourceTaskPoolManager.shuntDownNow(configuration.getProName());
                        TargetTaskPoolManager.shuntDownNow(configuration.getProName());
                        SysPoolManager.shuntDownNow(configuration.getProName());
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    MongoDbConnection.close(configuration.getSourceDsName());
                    MongoDbConnection.close(configuration.getTargetDsName());
                    MySqlConnection.close("1");
                    System.out.println("procName:" + configuration.getProName() + "关闭成功");

                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(configuration.getProName());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

}
