import cache.MemoryCache;
import common.OplogMetadata;
import conf.Configuration;
import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import execute.MongodbSource;
import execute.MongodbTarget;
import execute.MysqlSource;
import execute.MysqlTarget;
import task.*;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import util.Log;


/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMain {
    public static void main(String[] args) {

        testMysqlToMongoDb();


    }

    public static void testRealTimeOfMongodb() {
        Configuration configuration = ConfigurationUtil.getConfiguration("proc3");
        configuration.setDbTableWhite("\\w.+");
        MongoDbConnection.getMongoClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getSourceDsName()));

        MongoDbConnection.getMongoClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getTargetDsName()));


        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(), configuration.getSourceThreadNum(), configuration.getSourceThreadNum());
        SourceTaskPoolManager.addSourceTaskPoolManager(configuration.getProName(), sourceTaskPoolManager);


        TargetTaskPoolManager targetTaskPoolManager = new
                TargetTaskPoolManager(configuration.getProName(), 10,
                10);
        TargetTaskPoolManager.addTargetTaskPoolManager(configuration.getProName(), targetTaskPoolManager);

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

        MongoDbConnection.getMongoClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getSourceDsName()));

        MySqlConnection.getJdbcTemplate(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getTargetDsName()));

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

    public static void testMysqlToMongoDb() {
        //获取配置
        Configuration configuration = ConfigurationUtil.getConfiguration("proc2");
        configuration.setDbTableWhite("community.sys_user.*");
//        configuration.setSplitPk("dict_id");
//        configuration.setDbTableWhite("\\w.+");

        //mysql 源连接
        MySqlConnection.getJdbcTemplate(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getSourceDsName()));
        //mongodb 目标连接
        MongoDbConnection.getMongoClient(configuration.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getTargetDsName()));
        //缓存
        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(),
                configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);
        //配置缓存
        configuration.setMemoryCache(memoryCache);
        //源线程池
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(),
                3, 3);
        SourceTaskPoolManager.addSourceTaskPoolManager(configuration.getProName(), sourceTaskPoolManager);
        //系统线程池
        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 5, 5);
        SysPoolManager.addSysTaskPoolManager(configuration.getProName(), sysPoolManager);
        //目标线程池
        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(configuration.getProName(),
                5, 5);
        TargetTaskPoolManager.addTargetTaskPoolManager(configuration.getProName(), targetTaskPoolManager);

        MongodbTarget mongodbTarget = new MongodbTarget(configuration, memoryCache, configuration.getProName());
        mongodbTarget.startToTarget();

        MysqlSource mysqlSource = new MysqlSource(configuration, memoryCache);
        mysqlSource.createTask();

        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(configuration.getProName(), 0);
                boolean getAllDbTable = mysqlSource.isGetAllDbTable();
                int sourceTaskQueueSize = mysqlSource.getTaskMetadataQueueSize();
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
