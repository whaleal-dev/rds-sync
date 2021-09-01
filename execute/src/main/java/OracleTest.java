import cache.MemoryCache;
import com.whaleal.photon.source.oracle.execute.OracleSource;
import conf.Configuration;
import configuration.ConfigurationUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.oracle.OracleConnection;
import dbconnection.pgserver.PgServerConnection;
import execute.MongodbTarget;
import execute.PgSource;
import task.MongodbTargetTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import util.Log;

import java.util.List;
import java.util.Map;

public class OracleTest {
    public static void main(String[] args) throws InterruptedException {
        testOracleToMongoDb();


//        List<Map<String, Object>> mapList = OracleConnection.getJdbcTemplate("pg").queryForList("select * from USER ");
//
//        for (Map<String, Object> map : mapList) {
//            System.out.println(map.get("intclmn").getClass());
//        }

    }

    public static void testOracleToMongoDb(){
        //获取配置
        Configuration configuration = ConfigurationUtil.getConfiguration("proc4");


        OracleConnection.createConnection(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));

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

        OracleSource oracleSource = new OracleSource(configuration, memoryCache);
        oracleSource.createTask();

        while (true) {
            try {
                Thread.sleep(10000);
                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(configuration.getProName(), 0);
                boolean getAllDbTable = oracleSource.isGetAllDbTable();
                int sourceTaskQueueSize = oracleSource.getTaskMetadataQueueSize();
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
