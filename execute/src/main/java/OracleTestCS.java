//import com.whaleal.photon.common.cache.MemoryCache;
//import com.whaleal.photon.source.oracle.com.whaleal.photon.target.mongodb.execute.OracleAbstractSource;
//import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
//import com.whaleal.photon.core.datasource.DataSourceUtil;
//import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
//import com.whaleal.photon.core.dbconnection.oracle.OracleConnection;
//import com.whaleal.photon.target.mongodb.execute.MongodbTargetExecute;
//import com.whaleal.photon.core.programInfo.ProgramInfoUtil;
//import com.whaleal.photon.source.mysql.task.MongodbTargetTask;
//import com.whaleal.photon.common.thread.SourceTaskPoolManager;
//import com.whaleal.photon.common.thread.SysPoolManager;
//import com.whaleal.photon.common.thread.TargetTaskPoolManager;
//import com.whaleal.photon.common.util.Log;
//
//public class OracleTestCS {
//    public static void main(String[] args) throws InterruptedException {
//        testOracleToMongoDb();
//    }
//
//    public static void testOracleToMongoDb() {
//        //获取配置
//        ProgramInfo com.whaleal.photon.core.programInfo = ProgramInfoUtil.getProgramInfo("proc4");
//
//
//        OracleConnection.createConnection(com.whaleal.photon.core.programInfo.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(com.whaleal.photon.core.programInfo.getSourceDsName()));
//
//        MongoDbConnection.createMonoDbClient(com.whaleal.photon.core.programInfo.getTargetDsName(), DataSourceUtil.getDataSourceByDsName(com.whaleal.photon.core.programInfo.getTargetDsName()));
//        //缓存
//        MemoryCache memoryCache = new MemoryCache(com.whaleal.photon.core.programInfo.getTaskName(),
//                com.whaleal.photon.core.programInfo.getProName(), com.whaleal.photon.core.programInfo.getCacheNum(), com.whaleal.photon.core.programInfo.getCacheSize(), true);
//        //配置缓存
//        com.whaleal.photon.core.programInfo.setMemoryCache(memoryCache);
//
//        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(com.whaleal.photon.core.programInfo.getProName(),
//                com.whaleal.photon.core.programInfo.getSourceThreadNum(), com.whaleal.photon.core.programInfo.getSourceThreadNum());
//
//        SysPoolManager sysPoolManager = new SysPoolManager(com.whaleal.photon.core.programInfo.getProName(), 5, 5);
//
//
//        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(com.whaleal.photon.core.programInfo.getProName(),
//                5, 5);
//
//
//        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(com.whaleal.photon.core.programInfo, memoryCache, com.whaleal.photon.core.programInfo.getProName());
//        mongodbTarget.startToTarget();
//
//        OracleAbstractSource oracleSource = new OracleAbstractSource(com.whaleal.photon.core.programInfo, memoryCache);
//        oracleSource.createTask();
//
//        while (true) {
//            try {
//                Thread.sleep(10000);
//                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(com.whaleal.photon.core.programInfo.getProName(), 0);
//                boolean getAllDbTable = oracleSource.isGetAllDbTable();
//                int sourceTaskQueueSize = oracleSource.getTaskMetadataQueueSize();
//                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(com.whaleal.photon.core.programInfo.getProName(), 0);
//                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(com.whaleal.photon.core.programInfo.getProName(), 0);
//                int allDataCacheNum = memoryCache.getAllDataCacheNum();
//                Log.info(setSysActiveThreadNum + "");
//                Log.info("sum:" + (sourceThread + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum));
//                Log.info("sourceThread:" + sourceThread);
//                Log.info("sourceTaskQueueSize:" + sourceTaskQueueSize);
//                Log.info("setSysActiveThreadNum:" + setSysActiveThreadNum);
//                Log.info("targetActiveThreadNum:" + targetActiveThreadNum);
//                Log.info("allDataCacheNum:" + allDataCacheNum);
//                Log.info("getAllDbTable:" + getAllDbTable);
//                if ((sourceThread + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum + targetActiveThreadNum) == 0 && getAllDbTable) {
//                    Thread.sleep(10000);
//                    try {
//                        TargetTaskPoolManager.destroy(com.whaleal.photon.core.programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    try {
//                        SourceTaskPoolManager.destroy(com.whaleal.photon.core.programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    try {
//                        SysPoolManager.destroy(com.whaleal.photon.core.programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    MongodbTargetTask.setIsStopFlagOfTarget(com.whaleal.photon.core.programInfo.getProName(), false);
//                    memoryCache.gcMemoryCache();
//                    Log.info("procName:" + com.whaleal.photon.core.programInfo.getProName() + "关闭成功");
//                    Thread.sleep(10000);
//                    break;
//                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
//                    MongodbTargetTask.setIsStopFlagOfTarget(com.whaleal.photon.core.programInfo.getProName(), true);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//
//}
