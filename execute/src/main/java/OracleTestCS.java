import cache.MemoryCache;
import com.whaleal.photon.source.oracle.execute.OracleAbstractSource;
import common.photonV.entity.ProgramInfo;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.oracle.OracleConnection;
import execute.MongodbTargetExecute;
import programInfo.ProgramInfoUtil;
import task.MongodbTargetTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;
import util.Log;

public class OracleTestCS {
    public static void main(String[] args) throws InterruptedException {
        testOracleToMongoDb();
    }

    public static void testOracleToMongoDb() {
        //获取配置
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc4");


        OracleConnection.createConnection(programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));

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


        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache, programInfo.getProName());
        mongodbTarget.startToTarget();

        OracleAbstractSource oracleSource = new OracleAbstractSource(programInfo, memoryCache);
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
