import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.columntype.DbTypeFlag;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.common.photonV.entity.TaskTrigger;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractTargetTask;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.core.dbconnection.hadoop.HadoopConnection;
import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import com.whaleal.photon.core.programInfo.ProgramInfoUtil;
import com.whaleal.photon.core.trigger.TriggerUtil;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import com.whaleal.photon.realtime.mongodb.task.OplogNsBucketTask;
import com.whaleal.photon.realtime.mongodb.task.OplogNsTask;
import com.whaleal.photon.realtime.mongodb.task.OplogReadTask;
import com.whaleal.photon.realtime.mongodb.task.OplogWriteTask;
import com.whaleal.photon.source.mongodb.exexute.MongodbSourceExecute;
import com.whaleal.photon.source.mysql.execute.MysqlSourceExecute;
import com.whaleal.photon.source.oracle.execute.OracleSourceExecute;
import com.whaleal.photon.source.pg.execute.PgSourceExecute;
import com.whaleal.photon.target.hdfs.execute.HdfsTargetExecute;
import com.whaleal.photon.target.mongodb.execute.*;

import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.thread.SysPoolManager;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.common.util.StringUtil;
import com.whaleal.photon.target.mysql.execute.MysqlTargetExecute;

import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/31 2:04 下午
 */
public class TestMainLhp {

    public static void main(String[] args) {

        // proc1 mongodb-mongodb 2.8w/s
        // proc2 mysql-mongodb 5w/s
        // proc5 pg-mongodb   1.7w/s
        // proc8 oracle-mongodb 3k/s

        // proc9 mysql-mysql      400/s
        // proc7 pg-mysql         400/s
        // proc10  oracle-mysql   300/s
        // proc14 mongodb-mysql   400/s
        // proc15 mongodb-hdfs   400/s
        // proc1 mongodb实时同步


        String[] procNameArray = new String[]{"proc15"};
        for (String procName : procNameArray) {
            try {
                long batchNo = 1;
                exe(procName, "taskName", batchNo);
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }

        //  testRealTimeOfMongodb();

    }

    public static void init(ProgramInfo programInfo, Datasource dbSource, Datasource dbTarget) {

        if (dbSource.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dbTarget.getType().equalsIgnoreCase(DbTypeFlag.MONGODB)) {
            programInfo.setUseDeFaultType(true);
        }
        //源端是否为rdb
        if (dbSource.getType().equalsIgnoreCase(DbTypeFlag.MYSQL) ||
                dbSource.getType().equalsIgnoreCase(DbTypeFlag.PG) ||
                dbSource.getType().equalsIgnoreCase(DbTypeFlag.ORACLE)) {
            programInfo.setRdbOfSource(true);
        }
        System.out.println("==============" + programInfo.isUseDeFaultType());
    }

    public static void exe(String procName, String taskName, long batchNo) throws InterruptedException {
        String procNameAndBatchNo = procName + batchNo;
        //创建pro
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo(procName);
        programInfo.setDropExistDbTable(true);
//        programInfo.setQuerySql("    {$match:{a:1}},{$sort:{b:-1}},{$limit:100}");
//        programInfo.setPreSql("{\"drop\":\"test1000\"}");
//        programInfo.setDbTableName("photon.test1000");
        if (programInfo == null || programInfo.getProName().length() == 0) {
            return;
        }
        programInfo.setBatchNo(batchNo);
        // programInfo.setTargetThreadNum(1);
        //获取数据源对象
        Datasource dataSourceDb = DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName());
        Datasource dataTargetDb = DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName());
        init(programInfo, dataSourceDb, dataTargetDb);
        System.out.println(programInfo);
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
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.HADOOP)) {
                    HadoopConnection.createHadoopFileSystem(procNameAndBatchNoAndDsName, dataSourceDb);
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
        } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.MONGODB) && dataTargetDb.getType().equalsIgnoreCase(DbTypeFlag.HADOOP)) {
            testMongoDbToHadoop(programInfo, memoryCache, taskTrigger);
        }


    }

    public static void createThreadPoolManager(ProgramInfo programInfo) {
        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(procNameAndBatchNo, programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());

        SysPoolManager sysPoolManager = new SysPoolManager(procNameAndBatchNo, 3, 3);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(procNameAndBatchNo, programInfo.getTargetThreadNum(), programInfo.getTargetThreadNum());

    }

    public static void getProExeInfo(ProgramInfo programInfo, AbstractSourceExecute abstractSourceExecute, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0);
                boolean getAllDbTable = abstractSourceExecute.isGetAllDbTable();
                int sourceTaskQueueSize = abstractSourceExecute.getTaskMetadataQueueSize();
                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0);
                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                int allDataCacheNum = memoryCache.getAllDataCacheNum();
                Log.info(procNameAndBatchNo + ",读取线程数:" + sourceThread);
                Log.info(procNameAndBatchNo + ",剩余读取线程队列:" + sourceTaskQueueSize);
                Log.info(procNameAndBatchNo + ",写入线程数:" + targetActiveThreadNum);
                Log.info(procNameAndBatchNo + ",剩余缓存数:" + allDataCacheNum);
                Log.info(procNameAndBatchNo + ",Sys线程数:" + setSysActiveThreadNum);
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
                    Log.info(procNameAndBatchNo + ",状态" + state);
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
                } else if (dataSourceDb.getType().equalsIgnoreCase(DbTypeFlag.HADOOP)) {
                    HadoopConnection.close(procNameAndBatchNoAndDsName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    public static void testRealTimeOfMongodb() {
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc1");
        programInfo.setBatchNo(System.currentTimeMillis());
        programInfo.setDbTableWhite("photon.+");
        System.out.println(1);

        String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();

        programInfo.setRealTimeThreadNum(5);

        MongoDbConnection.createMonoDbClient(procNameAndBatchNo + programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));
        MongoDbConnection.createMonoDbClient(procNameAndBatchNo + programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));
        System.out.println(2);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(procNameAndBatchNo, programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());
        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(procNameAndBatchNo, 10, 10);

        System.out.println(3);
        OplogMetadata oplogMetadata = new OplogMetadata(programInfo);
        System.out.println(4);
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogWriteTask(oplogMetadata));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsTask(programInfo.getDbTableWhite(), oplogMetadata));
        TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogReadTask(programInfo, (int) (System.currentTimeMillis() / 1000), 0, 0, oplogMetadata));
        for (int i = 0; i < 4; i++) {
            TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogWriteTask(oplogMetadata));
            TargetTaskPoolManager.submit(procNameAndBatchNo, new OplogNsBucketTask(oplogMetadata, programInfo.getCacheSize()));
        }
        System.out.println(5);

    }

    public static void testMongoDbToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {

        MysqlTargetExecute mysqlTarget = new MysqlTargetExecute(programInfo, memoryCache);
        MongodbSourceExecute mongodbSource = new MongodbSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        mongodbSource.getAllDbTables();

        Set<String> dbTableNameSet = mongodbSource.getDbTableNameSet();

        // 简单查询任务   mongodb-mysql 暂不支持该功能
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mysqlTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            mongodbSource.executeQueryTask();
            mongodbSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            if (programInfo.isDropExistDbTable()) {
                mysqlTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                mysqlTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            mongodbSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            mongodbSource.splitDbTable();
        }
        mysqlTarget.start();
        getProExeInfo(programInfo, mongodbSource, memoryCache, taskTrigger);

    }

    public static void testPgToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        PgSourceExecute pgSource = new PgSourceExecute(programInfo, memoryCache);
        MysqlTargetExecute mysqlTarget = new MysqlTargetExecute(programInfo, memoryCache);
        //获取pg的同步的库表
        pgSource.getAllDbTables();
        //获取pg的同步的库表Set
        Set<String> dbTableNameSet = pgSource.getDbTableNameSet();
        // 简单查询任务
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mysqlTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            pgSource.executeQueryTask();
            pgSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            //把已经存在的target表进行删除
            if (programInfo.isDropExistDbTable()) {
                mysqlTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                //回滚该批次的数据
                mysqlTarget.rollBackDataFromDbTable(dbTableNameSet);
                //判断回滚任务是否完成
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            pgSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            pgSource.splitDbTable();
        }
        mysqlTarget.start();
        getProExeInfo(programInfo, pgSource, memoryCache, taskTrigger);
    }

    public static TaskTrigger generateTaskTriggerInfo(String taskName, String procName, long batchNo) {
        TaskTrigger taskTrigger = new TaskTrigger();
        taskTrigger.setId(StringUtil.generateUUID());
        taskTrigger.setBatchNo(batchNo);
        taskTrigger.setTaskName(taskName);
        taskTrigger.setProName(procName);
        return taskTrigger;
    }

    public static void testMongoDbToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache);
        MongodbSourceExecute mongodbSource = new MongodbSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        mongodbSource.getAllDbTables();
        Set<String> dbTableNameSet = mongodbSource.getDbTableNameSet();
        // 简单查询任务
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mongodbTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            mongodbSource.executeQueryTask();
            mongodbSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            if (programInfo.isDropExistDbTable()) {
                mongodbTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                mongodbTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            mongodbSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            mongodbSource.splitDbTable();
        }
        mongodbTarget.start();
        getProExeInfo(programInfo, mongodbSource, memoryCache, taskTrigger);
    }

    public static void testMysqlToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache);
        MysqlSourceExecute mysqlSource = new MysqlSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        mysqlSource.getAllDbTables();
        Set<String> dbTableNameSet = mysqlSource.getDbTableNameSet();
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mongodbTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            mysqlSource.executeQueryTask();
            mysqlSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            if (programInfo.isDropExistDbTable()) {
                mongodbTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                mongodbTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            mysqlSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            mysqlSource.splitDbTable();
        }
        mongodbTarget.start();
        getProExeInfo(programInfo, mysqlSource, memoryCache, taskTrigger);
    }

    public static void testPgToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache);
        PgSourceExecute pgSource = new PgSourceExecute(programInfo, memoryCache);
        pgSource.getAllDbTables();
        Set<String> dbTableNameSet = pgSource.getDbTableNameSet();
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mongodbTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            pgSource.executeQueryTask();
            pgSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            if (programInfo.isDropExistDbTable()) {
                mongodbTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                mongodbTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }

            // 启动获取提交Task任务的线程
            pgSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            pgSource.splitDbTable();
        }
        mongodbTarget.start();
        getProExeInfo(programInfo, pgSource, memoryCache, taskTrigger);
    }

    public static void testOracleToMongoDb(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {


        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache);

        OracleSourceExecute oracleSource = new OracleSourceExecute(programInfo, memoryCache);
        oracleSource.getAllDbTables();


        Set<String> dbTableNameSet = oracleSource.getDbTableNameSet();


        if (programInfo.isDropExistDbTable()) {
            mongodbTarget.deleteExistDbTable(dbTableNameSet);
        } else {
            mongodbTarget.rollBackDataFromDbTable(dbTableNameSet);
            String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
            int targetActiveThreadNum = 0;
            do {
                targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {

                }
            } while (targetActiveThreadNum != 0);
        }


        // 启动获取提交Task任务的线程
        oracleSource.submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        oracleSource.splitDbTable();
        mongodbTarget.start();
        getProExeInfo(programInfo, oracleSource, memoryCache, taskTrigger);
    }

    public static void testMysqlToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        MysqlTargetExecute mysqlTarget = new MysqlTargetExecute(programInfo, memoryCache);
        MysqlSourceExecute mysqlSource = new MysqlSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        mysqlSource.getAllDbTables();
        Set<String> dbTableNameSet = mysqlSource.getDbTableNameSet();
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mysqlTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            mysqlSource.executeQueryTask();
            mysqlSource.setGetAllDbTable(true);
        } else {
            if (programInfo.isDropExistDbTable()) {
                mysqlTarget.deleteExistDbTable(dbTableNameSet);
                Map<String, String> tableStructure = mysqlSource.getTableStructure();
                mysqlTarget.createTable(tableStructure);
                try {
                    Thread.sleep(10000);
                } catch (Exception e) {

                }
            } else {
                mysqlTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            mysqlSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            mysqlSource.splitDbTable();
        }
        //开始target启动任务
        mysqlTarget.start();
        getProExeInfo(programInfo, mysqlSource, memoryCache, taskTrigger);
    }

    public static void testOracleToMysql(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        MysqlTargetExecute mysqlTarget = new MysqlTargetExecute(programInfo, memoryCache);
        OracleSourceExecute oracleSource = new OracleSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        oracleSource.getAllDbTables();
        Set<String> dbTableNameSet = oracleSource.getDbTableNameSet();
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                mysqlTarget.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            oracleSource.executeQueryTask();
            oracleSource.setGetAllDbTable(true);
        } else {

            if (programInfo.isDropExistDbTable()) {
                mysqlTarget.deleteExistDbTable(dbTableNameSet);
            } else {
                mysqlTarget.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            oracleSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            oracleSource.splitDbTable();
        }
        mysqlTarget.start();
        getProExeInfo(programInfo, oracleSource, memoryCache, taskTrigger);
    }

    public static void testMongoDbToHadoop(ProgramInfo programInfo, MemoryCache memoryCache, TaskTrigger taskTrigger) {
        HdfsTargetExecute hdfsTargetExecute = new HdfsTargetExecute(programInfo, memoryCache);
        MongodbSourceExecute mongodbSource = new MongodbSourceExecute(programInfo, memoryCache);
        // 获取数据源的全部库表
        mongodbSource.getAllDbTables();
        Set<String> dbTableNameSet = mongodbSource.getDbTableNameSet();
        // 简单查询任务
        if (programInfo.getQuerySql() != null && programInfo.getQuerySql().length() > 2) {
            //执行target前置sql
            if (programInfo.getPreSql() != null && programInfo.getPreSql().length() > 2) {
                hdfsTargetExecute.executePreSql(programInfo.getPreSql());
            }
            //执行简单查询语句
            mongodbSource.executeQueryTask();
            mongodbSource.setGetAllDbTable(true);
            //开始target启动任务
        } else {
            if (programInfo.isDropExistDbTable()) {
                hdfsTargetExecute.deleteExistDbTable(dbTableNameSet);
            } else {
                hdfsTargetExecute.rollBackDataFromDbTable(dbTableNameSet);
                String procNameAndBatchNo = programInfo.getProName() + programInfo.getBatchNo();
                int targetActiveThreadNum = 0;
                do {
                    targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(procNameAndBatchNo, 0);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {

                    }
                } while (targetActiveThreadNum != 0);
            }
            // 启动获取提交Task任务的线程
            mongodbSource.submitSourceTask();
            // 开始遍历抽取该数据源的所有库表
            mongodbSource.splitDbTable();
        }
        hdfsTargetExecute.start();
        getProExeInfo(programInfo, mongodbSource, memoryCache, taskTrigger);
    }

}
