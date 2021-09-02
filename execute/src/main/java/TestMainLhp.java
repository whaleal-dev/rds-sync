import cache.MemoryCache;
import com.whaleal.photon.source.oracle.execute.OracleSource;
import common.OplogMetadata;
import common.dbtype.DbTypeFlag;
import common.photonV.entity.Datasource;
import common.photonV.entity.TaskTrigger;
import common.photonV.entity.ProgramInfo;
import dbconnection.oracle.OracleConnection;
import programInfo.ProgramInfoUtil;
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

        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc2");

        Datasource dataSourceDb = DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName());
        Datasource dataTargetDb = DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName());
        createDataSourceConnection(dataSourceDb, dataTargetDb);

        TaskTrigger taskTrigger = generateTaskTriggerInfo(programInfo.getTaskName(), programInfo.getProName());
        TriggerUtil.insertTriggerInfo(taskTrigger);

        createThreadPoolManager(programInfo);

        if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMongoDbToMongoDb(programInfo, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMysqlToMongoDb(programInfo, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testPgToMongoDb(programInfo, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testOracleToMongoDb(programInfo, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testMongoDbToMysql(programInfo, taskTrigger);
        }
    }

    public static void createDataSourceConnection(Datasource... dataSourceList) {
        for (Datasource dataSourceDb : dataSourceList) {
            try {
                if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
                    MongoDbConnection.createMonoDbClient(dataSourceDb.getName(), dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
                    MySqlConnection.createConnection(dataSourceDb.getName(), dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
                    PgServerConnection.createConnection(dataSourceDb.getName(), dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
                    OracleConnection.createConnection(dataSourceDb.getName(), dataSourceDb);
                }
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    public static void createThreadPoolManager(ProgramInfo programInfo) {
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(), programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 2, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(), programInfo.getTargetThreadNum(), programInfo.getTargetThreadNum());

    }

    public static void testRealTimeOfMongodb() {
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc3");
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

    public static void testMongoDbToMysql(ProgramInfo programInfo, TaskTrigger taskTrigge) {

        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);

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
                    TargetTaskPoolManager.destroy(programInfo.getProName());
                    SourceTaskPoolManager.destroy(programInfo.getProName());
                    SysPoolManager.destroy(programInfo.getProName());

                    MysqlTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();

                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
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

    public static void testMongoDbToMongoDb(ProgramInfo programInfo, TaskTrigger taskTrigger) {

        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);

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
                Log.info("sourceThread:   " + sourceThread);
                Log.info("sourceTaskQueueSize:   " + sourceTaskQueueSize);
                Log.info("setSysActiveThreadNum:   " + setSysActiveThreadNum);
                Log.info("targetActiveThreadNum:   " + targetActiveThreadNum);
                Log.info("allDataCacheNum:   " + allDataCacheNum);
                Log.info("getAllDbTable:   " + getAllDbTable);
                if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
                    Thread.sleep(10000);
                    TargetTaskPoolManager.destroy(programInfo.getProName());
                    SourceTaskPoolManager.destroy(programInfo.getProName());
                    SysPoolManager.destroy(programInfo.getProName());

                    memoryCache.gcMemoryCache();
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);

                    taskTrigger.setState("success");

                    TriggerUtil.updateTriggerInfo(taskTrigger);
                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void testMysqlToMongoDb(ProgramInfo programInfo, TaskTrigger taskTrigger) {

        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);

        programInfo.setMemoryCache(memoryCache);

        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(programInfo, memoryCache);
        mysqlSource.createTask();

        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = mysqlSource.isGetAllDbTable();
                int sourceTaskQueueSize = mysqlSource.getTaskMetadataQueueSize();
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
                    TargetTaskPoolManager.destroy(programInfo.getProName());
                    SourceTaskPoolManager.destroy(programInfo.getProName());
                    SysPoolManager.destroy(programInfo.getProName());

                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();

                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void testPgToMongoDb(ProgramInfo programInfo, TaskTrigger taskTrigger) {
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);

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
                    TargetTaskPoolManager.destroy(programInfo.getProName());
                    SourceTaskPoolManager.destroy(programInfo.getProName());
                    SysPoolManager.destroy(programInfo.getProName());

                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();

                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void testOracleToMongoDb(ProgramInfo programInfo, TaskTrigger taskTrigger) {
        //缓存
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);

        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        OracleSource oracleSource = new OracleSource(programInfo, memoryCache);
        oracleSource.createTask();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = oracleSource.isGetAllDbTable();
                int sourceTaskQueueSize = oracleSource.getTaskMetadataQueueSize();
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
                    TargetTaskPoolManager.destroy(programInfo.getProName());
                    SourceTaskPoolManager.destroy(programInfo.getProName());
                    SysPoolManager.destroy(programInfo.getProName());

                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();

                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
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
