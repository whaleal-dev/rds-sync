import cache.MemoryCache;
import common.OplogMetadata;
import common.photonV.entity.TaskTrigger;
import common.photonV.entity.ProgramInfo;
import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import dbconnection.pgserver.PgServerConnection;
import execute.*;
import task.*;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import trigger.TriggerUtil;
import util.Log;
import util.StringUtil;

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
        ProgramInfo programInfo = ConfigurationUtil.getConfiguration("proc3");
        programInfo.setDbTableWhite("\\w.+");
        MongoDbConnection.createMonoDbClient(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));


        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(), programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(programInfo.getProName(), 10,
                10);

        OplogMetadata oplogMetadata = new OplogMetadata(programInfo);

        TargetTaskPoolManager.submit(programInfo.getProName(), new OplogWriteTask(oplogMetadata));
        TargetTaskPoolManager.submit(programInfo.getProName(), new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        TargetTaskPoolManager.submit(programInfo.getProName(), new OplogNsTask(programInfo.getDbTableWhite(), oplogMetadata));
        TargetTaskPoolManager.submit(programInfo.getProName(), new OplogReadTask(programInfo, (int) (System.currentTimeMillis() / 1000)
                , 0, 0, oplogMetadata));
        for (int i = 0; i < 2; i++) {
            TargetTaskPoolManager.submit(programInfo.getProName(), new OplogWriteTask(oplogMetadata));
            TargetTaskPoolManager.submit(programInfo.getProName(), new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        }

    }

    public static void testMongoDbToMysql() {

        ProgramInfo programInfo = ConfigurationUtil.getConfiguration("proc2");

        MongoDbConnection.createMonoDbClient(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

        MySqlConnection.createConnection(programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));

        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);


        programInfo.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(), programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 2, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(), 10, 10);

        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = mongodbSource.isGetAllDbTable();
                int sourceTaskQueueSize = mongodbSource.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(programInfo.getProName(), 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(programInfo.getProName(), 0);
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
                        TargetTaskPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    MysqlTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();
                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    Thread.sleep(10000);
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MysqlTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
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


        ProgramInfo programInfo = ConfigurationUtil.getConfiguration("proc1");
        TaskTrigger taskTrigger = generateTaskTriggerInfo(programInfo.getTaskName(), programInfo.getProName());
        TriggerUtil.insertTriggerInfo(taskTrigger);
        MongoDbConnection.createMonoDbClient(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));

        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(),
                programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);


        programInfo.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(), programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());


        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 2, 3);


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(programInfo.getProName(), 1,
                1);


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = mongodbSource.isGetAllDbTable();
                int sourceTaskQueueSize = mongodbSource.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(programInfo.getProName(), 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(programInfo.getProName(), 0);
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

                        TargetTaskPoolManager.destroy(programInfo.getProName());

                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    memoryCache.gcMemoryCache();
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
//                    MongoDbConnection.close(programInfo.getTargetName());
//                    MongoDbConnection.close(programInfo.getSourceName());
                    //MySqlConnection.close("1");
                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    taskTrigger.setState("success");
                    TriggerUtil.updateTriggerInfo(taskTrigger);
                    Thread.sleep(10000);
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    public static void testPgToMongoDb() {
        //获取配置
        ProgramInfo programInfo = ConfigurationUtil.getConfiguration("proc5");


        PgServerConnection.createConnection(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));
        //缓存
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(),
                programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        //配置缓存
        programInfo.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(),
                programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 5, 5);


        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(),
                5, 5);


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        PgSource pgSource = new PgSource(programInfo, memoryCache);
        pgSource.createTask();

        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = pgSource.isGetAllDbTable();
                int sourceTaskQueueSize = pgSource.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(programInfo.getProName(), 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(programInfo.getProName(), 0);
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
                        TargetTaskPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SourceTaskPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    try {
                        SysPoolManager.destroy(programInfo.getProName());
                    } catch (Exception e) {
                        Log.info(e.getMessage());
                    }
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();
                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    Thread.sleep(10000);
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }


}
