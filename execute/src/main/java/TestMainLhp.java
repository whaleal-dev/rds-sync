import cache.MemoryCache;
import com.whaleal.photon.source.oracle.execute.OracleSource;
import common.OplogMetadata;
import common.dbtype.DbTypeFlag;
import common.photonV.entity.Datasource;
import common.photonV.entity.TaskTrigger;
import common.photonV.entity.ProgramInfo;
import common.taskbase.AbstractTargetTask;
import common.taskbase.metadata.SourceMetadata;
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

    public static void main(String[] args) {

        String[] procNameArray = new String[]{"proc7"};

        for (String procName : procNameArray) {
            try {
                long batchNo = System.currentTimeMillis();
                exe(procName, "taskName", batchNo);
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
          }
    //    testRealTimeOfMongodb();


    }

    public static void exe(String procName, String taskName, long batchNo) throws InterruptedException {
        String procNameAndBatchNo = procName + batchNo;
        //创建pro
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo(procName);
        if (programInfo == null || programInfo.getProName().length() == 0) {
            return;
        }
        programInfo.setBatchNO(batchNo);
        programInfo.setTargetThreadNum(1);
        //获取数据源对象
        Datasource dataSourceDb = DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName());
        Datasource dataTargetDb = DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName());
        //创建数据源链接
        createDataSourceConnection(procNameAndBatchNo, dataSourceDb, dataTargetDb);
        //创建触发对象实例
        TaskTrigger taskTrigger = generateTaskTriggerInfo(taskName, programInfo.getProName(), batchNo);

        TriggerUtil.insertTriggerInfo(taskTrigger);
        //创建线程池
        createThreadPoolManager(programInfo);
        //创建缓存类
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);
        // 开始传输数据
        generateTranT(dataSourceDb, dataTargetDb, programInfo, taskTrigger, memoryCache);
        disDataSourceConnection(procNameAndBatchNo, dataSourceDb, dataTargetDb);
        destroy(procName, batchNo, memoryCache);
        TriggerUtil.updateTriggerInfo(taskTrigger);
    }

    public static void createDataSourceConnection(String procNameAndBatchNo, Datasource... dataSourceList) {

        for (Datasource dataSourceDb : dataSourceList) {
            String procNameAndBatchNoAndDsName = procNameAndBatchNo + dataSourceDb.getName();
            try {
                if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
                    MongoDbConnection.createMonoDbClient(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
                    MySqlConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
                    PgServerConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
                    OracleConnection.createConnection(procNameAndBatchNoAndDsName, dataSourceDb);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    public static void generateTranT(Datasource dataSourceDb, Datasource dataTargetDb, ProgramInfo programInfo, TaskTrigger taskTrigger, MemoryCache memoryCache) {
        if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMongoDbToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMysqlToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testPgToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testOracleToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testOracleToMysql(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testMongoDbToMysql(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testPgToMysql(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testMysqlToMysql(programInfo, memoryCache, taskTrigger);
        }
    }

    public static void createThreadPoolManager(ProgramInfo programInfo) {
        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNO();
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(procNameAndBatchNo, programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(procNameAndBatchNo, 2, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(procNameAndBatchNo, programInfo.getTargetThreadNum(), programInfo.getTargetThreadNum());

    }

    public static void getProExeInfo(ProgramInfo programInfo, SourceMetadata sourceMetadata, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNO();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0);
                boolean getAllDbTable = sourceMetadata.isGetAllDbTable();
                int sourceTaskQueueSize = sourceMetadata.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                int allDataCacheNum = memoryCache.getAllDataCacheNum();
                Log.info(procNameAndBatchNo + ",读取线程数:" + sourceThread);
                Log.info(procNameAndBatchNo + ",剩余读取线程队列:" + sourceTaskQueueSize);
                Log.info(procNameAndBatchNo + ",写入线程数:" + targetActiveThreadNum);
                Log.info(procNameAndBatchNo + ",剩余缓存数:" + allDataCacheNum);
                taskTrigger.setState("run");
                TriggerUtil.updateTriggerInfo(taskTrigger);
                if ((sourceThread + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
                    Thread.sleep(10000);
                    taskTrigger.setState("success");
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    AbstractTargetTask.setIsStopFlagOfTarget(procNameAndBatchNo, true);
                } else {
                    String state = TriggerUtil.getTriggerTateInfo(taskTrigger.getId());
                    Log.error(procNameAndBatchNo + ",状态" + state);
                    if (state == null || state.equals("") || state.equals("stop")) {
                        AbstractTargetTask.setIsStopFlagOfTarget(procNameAndBatchNo, true);
                        taskTrigger.setState("stop");
                        Thread.sleep(10000);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void destroy(String procName, long batchNo, MemoryCache memoryCache) {
        String procNameAndBatchNo = procName + batchNo;

        TargetTaskPoolManager.destroy(procNameAndBatchNo);
        SourceTaskPoolManager.destroy(procNameAndBatchNo);
        SysPoolManager.destroy(procNameAndBatchNo);

        AbstractTargetTask.removeIsStopFlagOfTarget(procNameAndBatchNo);

        memoryCache.gcMemoryCache();
        Log.error("procName:" + procNameAndBatchNo + "关闭成功");
    }

    public static void disDataSourceConnection(String procNameAndBatchNo, Datasource... dataSourceList) {
        for (Datasource dataSourceDb : dataSourceList) {
            String procNameAndBatchNoAndDsName = procNameAndBatchNo + dataSourceDb.getName();
            try {
                if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
                    MongoDbConnection.close(procNameAndBatchNoAndDsName);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
                    MySqlConnection.close(procNameAndBatchNoAndDsName);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG)) {
                    PgServerConnection.close(procNameAndBatchNoAndDsName);
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
                    OracleConnection.close(procNameAndBatchNoAndDsName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    public static void testRealTimeOfMongodb() {
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc1");
        programInfo.setDbTableWhite("photon.+");


        programInfo.setIncrementParseThreadNum(5);

        MongoDbConnection.createMonoDbClient(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNO();


        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));


        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(procNameAndBatchNo, programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(procNameAndBatchNo, 10,
                10);

        OplogMetadata oplogMetadata = new OplogMetadata(programInfo);

        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogWriteTask(oplogMetadata));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsTask(programInfo.getDbTableWhite(), oplogMetadata));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogReadTask(programInfo, (int) (System.currentTimeMillis() / 1000)

                , 0, 0, oplogMetadata));
        for (int i = 0; i < 2; i++) {
            TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogWriteTask(oplogMetadata));
            TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        }

    }

    public static void testMongoDbToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        getProExeInfo(programInfo, mongodbSource, memoryCache, taskTrigger);
    }

    public static void testPgToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        PgSource pgSource = new PgSource(programInfo, memoryCache);
        pgSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        getProExeInfo(programInfo, pgSource, memoryCache, taskTrigger);
    }

    public static TaskTrigger generateTaskTriggerInfo(String taskName, String procName, long batchNo) {
        TaskTrigger taskTrigger = new TaskTrigger();
        taskTrigger.setId(StringUtil.generateUUID());
        taskTrigger.setBatchNo(batchNo);
        taskTrigger.setTaskName(taskName);
        taskTrigger.setProcName(procName);
        return taskTrigger;
    }

    public static void testMongoDbToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();

        getProExeInfo(programInfo, mongodbSource, memoryCache, taskTrigger);
    }

    public static void testMysqlToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {

        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(programInfo, memoryCache);
        mysqlSource.createTask();

        getProExeInfo(programInfo, mysqlSource, memoryCache, taskTrigger);
    }

    public static void testPgToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        PgSource pgSource = new PgSource(programInfo, memoryCache);
        pgSource.createTask();

        getProExeInfo(programInfo, pgSource, memoryCache, taskTrigger);
    }

    public static void testOracleToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        OracleSource oracleSource = new OracleSource(programInfo, memoryCache);
        oracleSource.createTask();
        getProExeInfo(programInfo, oracleSource, memoryCache, taskTrigger);
    }

    public static void testMysqlToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(programInfo, memoryCache);
        mysqlSource.createTask();
        getProExeInfo(programInfo, mysqlSource, memoryCache, taskTrigger);
    }

    public static void testOracleToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        OracleSource oracleSource = new OracleSource(programInfo, memoryCache);
        oracleSource.createTask();
        getProExeInfo(programInfo, oracleSource, memoryCache, taskTrigger);
    }


}
