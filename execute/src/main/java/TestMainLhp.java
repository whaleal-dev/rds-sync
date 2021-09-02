import cache.MemoryCache;
import common.OplogMetadata;
import common.photonV.entity.TaskTrigger;
import conf.Configuration;
import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import dbconnection.pgserver.PgServerConnection;
import execute.*;
import org.postgresql.util.PGobject;
import task.*;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import trigger.TriggerUtil;
import util.Log;
import util.StringUtil;

import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/31 2:04 下午
 */
public class TestMainLhp {
    public static void main(String[] args) throws InterruptedException {
        testMongoDbToMongoDb();




    }

    public static void testRealTimeOfMongodb() {
        Configuration configuration = ConfigurationUtil.getConfiguration("proc3");
        configuration.setDbTableWhite("\\w.+");
        MongoDbConnection.createMonoDbClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getTargetDsName()));


        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(), configuration.getSourceThreadNum(), configuration.getSourceThreadNum());


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(configuration.getProName(), 10,
                10);

        OplogMetadata oplogMetadata = new OplogMetadata(configuration);

        TargetTaskPoolManager.submit(configuration.getProName(), new OplogWriteTask(oplogMetadata));
        TargetTaskPoolManager.submit(configuration.getProName(), new OplogNsBucketTask(oplogMetadata, configuration.getCacheSize()));
        TargetTaskPoolManager.submit(configuration.getProName(), new OplogNsTask(configuration.getDbTableWhite(), oplogMetadata));
        TargetTaskPoolManager.submit(configuration.getProName(), new OplogReadTask(configuration, (int) (System.currentTimeMillis() / 1000)
                , 0, 0, oplogMetadata));
        for (int i = 0; i < 2; i++) {
            TargetTaskPoolManager.submit(configuration.getProName(), new OplogWriteTask(oplogMetadata));
            TargetTaskPoolManager.submit(configuration.getProName(), new OplogNsBucketTask(oplogMetadata, configuration.getCacheSize()));
        }

    }

    public static void testMongoDbToMysql() {

        Configuration configuration = ConfigurationUtil.getConfiguration("proc2");

        MongoDbConnection.createMonoDbClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));

        MySqlConnection.createConnection(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getTargetDsName()));

        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(), configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);


        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(), configuration.getSourceThreadNum(), configuration.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 2, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(configuration.getProName(), 10, 10);

        MongodbSource mongodbSource = new MongodbSource(configuration, memoryCache);
        mongodbSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(configuration, memoryCache, configuration.getProName());
        mysqlTarget.startToTarget();
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
                        TargetTaskPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    MysqlTargetTask.setIsStopFlagOfTarget(configuration.getProName(), false);
                    memoryCache.gcMemoryCache();
                    Log.info("procName:" + configuration.getProName() + "关闭成功");
                    Thread.sleep(10000);
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MysqlTargetTask.setIsStopFlagOfTarget(configuration.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public static TaskTrigger generateTaskTriggerInfo(String taskName, String procName) {
        TaskTrigger taskTrigger = new TaskTrigger();
        taskTrigger.setId(StringUtil.generateUUID());
        taskTrigger.setTaskName(taskName);
        taskTrigger.setProcName(procName);
        return taskTrigger;
    }


    public static void testMongoDbToMongoDb() {


        Configuration configuration = ConfigurationUtil.getConfiguration("proc1");
        TaskTrigger taskTrigger = generateTaskTriggerInfo(configuration.getTaskName(), configuration.getProName());
        TriggerUtil.insertTriggerInfo(taskTrigger);
        MongoDbConnection.createMonoDbClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getTargetDsName()));

        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(),
                configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);


        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(), configuration.getSourceThreadNum(), configuration.getSourceThreadNum());


        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 2, 3);


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(configuration.getProName(), 1,
                1);


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

                        TargetTaskPoolManager.destroy(configuration.getProName());

                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    memoryCache.gcMemoryCache();
                    MongodbTargetTask.setIsStopFlagOfTarget(configuration.getProName(), false);
//                    MongoDbConnection.close(configuration.getTargetName());
//                    MongoDbConnection.close(configuration.getSourceName());
                    //MySqlConnection.close("1");
                    Log.info("procName:" + configuration.getProName() + "关闭成功");
                    taskTrigger.setState("success");
                    TriggerUtil.updateTriggerInfo(taskTrigger);
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


    public static void testPgToMongoDb() {
        //获取配置
        Configuration configuration = ConfigurationUtil.getConfiguration("proc5");


        PgServerConnection.createConnection(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getTargetDsName()));
        //缓存
        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(),
                configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);
        //配置缓存
        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(),
                configuration.getSourceThreadNum(), configuration.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 5, 5);


        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(configuration.getProName(),
                5, 5);


        MongodbTarget mongodbTarget = new MongodbTarget(configuration, memoryCache, configuration.getProName());
        mongodbTarget.startToTarget();

        PgSource pgSource = new PgSource(configuration, memoryCache);
        pgSource.createTask();

        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(configuration.getProName(), 0);
                boolean getAllDbTable = pgSource.isGetAllDbTable();
                int sourceTaskQueueSize = pgSource.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(configuration.getProName(), 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(configuration.getProName(), 0);
                int allDataCacheNum = memoryCache.getAllDataCacheNum();
                Log.info(setSysActiveThreadNum + "");
                Log.info("sum:" + (sourceThread + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum));
                Log.info("sourceThread:" + sourceThread);
                Log.info("sourceTaskQueueSize:" + sourceTaskQueueSize);
                Log.info("setSysActiveThreadNum:" + setSysActiveThreadNum);
                Log.info("targetActiveThreadNum:" + targetActiveThreadNum);
                Log.info("allDataCacheNum:" + allDataCacheNum);
                Log.info("getAllDbTable:" + getAllDbTable);
                if ((sourceThread + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
                    Thread.sleep(10000);
                    try {
                        TargetTaskPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(configuration.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    MongodbTargetTask.setIsStopFlagOfTarget(configuration.getProName(), false);
                    memoryCache.gcMemoryCache();
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
