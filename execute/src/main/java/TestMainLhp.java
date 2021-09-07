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
    public static void main(String[] args) throws InterruptedException {
        //创建pro
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc6");
        //获取数据源对象
        Datasource dataSourceDb = DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName());
        Datasource dataTargetDb = DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName());
        //创建数据源链接
        createDataSourceConnection(dataSourceDb, dataTargetDb);
        //创建触发对象实例
        TaskTrigger taskTrigger = generateTaskTriggerInfo(programInfo.getTaskName(), programInfo.getProName());
        TriggerUtil.insertTriggerInfo(taskTrigger);
        //创建线程池
        createThreadPoolManager(programInfo);
        //创建缓存类
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(), programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        programInfo.setMemoryCache(memoryCache);
        // 开始传输数据
        if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMongoDbToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testMysqlToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testPgToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.ORACLE) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            testOracleToMongoDb(programInfo, memoryCache, taskTrigger);
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testMongoDbToMysql(programInfo, memoryCache, taskTrigger);
        }else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.PG) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testPgToMysql(programInfo, memoryCache, taskTrigger);
        }else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.MYSQL)) {
            testMysqlToMysql(programInfo, memoryCache, taskTrigger);
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
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    public static void createThreadPoolManager(ProgramInfo programInfo) {
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(), programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 2, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(), programInfo.getTargetThreadNum(), programInfo.getTargetThreadNum());

    }

    public static void getProExeInfo(ProgramInfo programInfo, SourceMetadata sourceMetadata, MemoryCache memoryCache) {
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
                boolean getAllDbTable = sourceMetadata.isGetAllDbTable();
                int sourceTaskQueueSize = sourceMetadata.getTaskMetadataQueueSize();
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

                    AbstractTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
                    memoryCache.gcMemoryCache();

                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
                    break;
                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
                    AbstractTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public static void testMongoDbToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigge) {


        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        getProExeInfo(programInfo, mongodbSource, memoryCache);
    }
    public static void testPgToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigge) {


        PgSource pgSource = new PgSource(programInfo, memoryCache);
        pgSource.createTask();

        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        getProExeInfo(programInfo, pgSource, memoryCache);
    }
    public static TaskTrigger generateTaskTriggerInfo(String taskName, String procName) {
        TaskTrigger taskTrigger = new TaskTrigger();
        taskTrigger.setId(StringUtil.generateUUID());
        taskTrigger.setTaskName(taskName);
        taskTrigger.setProcName(procName);
        return taskTrigger;
    }

    public static void testMongoDbToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MongodbSource mongodbSource = new MongodbSource(programInfo, memoryCache);
        mongodbSource.createTask();

        getProExeInfo(programInfo, mongodbSource, memoryCache);
    }

    public static void testMysqlToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {

        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(programInfo, memoryCache);
        mysqlSource.createTask();

        getProExeInfo(programInfo, mysqlSource, memoryCache);
    }

    public static void testPgToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        PgSource pgSource = new PgSource(programInfo, memoryCache);
        pgSource.createTask();

        getProExeInfo(programInfo, pgSource, memoryCache);
    }

    public static void testOracleToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTarget mongodbTarget = new MongodbTarget(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        OracleSource oracleSource = new OracleSource(programInfo, memoryCache);
        oracleSource.createTask();
        getProExeInfo(programInfo, oracleSource, memoryCache);
    }

    public static void testMysqlToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MysqlTarget mysqlTarget = new MysqlTarget(programInfo, memoryCache, programInfo.getProName());
        mysqlTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(programInfo, memoryCache);
        mysqlSource.createTask();
        getProExeInfo(programInfo, mysqlSource, memoryCache);
    }
}
