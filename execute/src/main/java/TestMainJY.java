//import com.whaleal.photon.common.cache.MemoryCache;
//import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
//import com.whaleal.photon.target.mongodb.execute.MongodbTargetExecute;
//import com.whaleal.photon.core.programInfo.ProgramInfoUtil;
//import com.whaleal.photon.core.datasource.DataSourceUtil;
//import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
//import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
//import com.whaleal.photon.target.mongodb.execute.MysqlAbstractSource;
//import com.whaleal.photon.source.mysql.task.*;
//import com.whaleal.photon.common.thread.SourceTaskPoolManager;
//import com.whaleal.photon.common.thread.SysPoolManager;
//import com.whaleal.photon.common.thread.TargetTaskPoolManager;
//import com.whaleal.photon.common.util.Log;
//
//
///**
// * @description:
// * @author: lhp
// * @time: 2021/8/23 3:29 下午
// */
//public class TestMainJY {
//    public static void main(String[] args) throws InterruptedException {
//
//        testMysqlToMongoDb();
//    }
//
//    public static void testMysqlToMongoDb() {
//        //获取配置
//        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc2");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_user.*");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_menu");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.community.banner)");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_user_token");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_captcha");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.test1");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_captcha");
////        com.whaleal.photon.core.programInfo.setAdviceNumber(4);
////        com.whaleal.photon.core.programInfo.setDbTableWhite("(community.community_dict)||(community.sys_captcha)");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("test.test");
//        programInfo.setDbTableWhite("community.test4");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.community_banner");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.test");
////        com.whaleal.photon.core.programInfo.setSplitPk("banner_link");
//        programInfo.setAdviceNumber(10);
////        com.whaleal.photon.core.programInfo.setSplitPk("id");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("community.sys_.*");
////        com.whaleal.photon.core.programInfo.setAdviceNumber(5);
//
////        com.whaleal.photon.core.programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.sys_user_token)");
////        com.whaleal.photon.core.programInfo.setAdviceNumber(5);
////        com.whaleal.photon.core.programInfo.setSplitPk("dict_id");
////        com.whaleal.photon.core.programInfo.setDbTableWhite("\\w.+");
//        //mysql 源连接
//        MySqlConnection.createConnection(programInfo.getSourceDsName(),
//                DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));
//        //mongodb 目标连接
//        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(),
//                DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));
//        //缓存
//        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(),
//                programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
//        //配置缓存
//        programInfo.setMemoryCache(memoryCache);
//        //源线程池
//        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(),
//                programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());
//        //系统线程池
//        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 5, 5);
//        //目标线程池
//        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(),
//                5, 5);
//
//        MongodbTargetExecute mongodbTarget = new MongodbTargetExecute(programInfo, memoryCache, programInfo.getProName());
//        mongodbTarget.startToTarget();
//
//        MysqlAbstractSource mysqlSource = new MysqlAbstractSource(programInfo, memoryCache);
//        mysqlSource.createTask();
//
//        while (true) {
//            try {
//                Thread.sleep(10000);
//                int sourceThread = SourceTaskPoolManager.setSourceActiveThreadNum(programInfo.getProName(), 0);
//                boolean getAllDbTable = mysqlSource.isGetAllDbTable();
//                int sourceTaskQueueSize = mysqlSource.getTaskMetadataQueueSize();
//                int setSysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(programInfo.getProName(), 0);
//                int targetActiveThreadNum = TargetTaskPoolManager.setTargetActiveThreadNum(programInfo.getProName(), 0);
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
//                        TargetTaskPoolManager.destroy(programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    try {
//                        SourceTaskPoolManager.destroy(programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    try {
//                        SysPoolManager.destroy(programInfo.getProName());
//                    } catch (Exception e) {
//                        Log.info(e.getMessage());
//                    }
//                    MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), false);
//                    memoryCache.gcMemoryCache();
//                    Log.info("procName:" + programInfo.getProName() + "关闭成功");
//                    Thread.sleep(10000);
//                    break;
//                } else if ((sourceThread + sourceTaskQueueSize + sourceTaskQueueSize + allDataCacheNum + setSysActiveThreadNum) == 0 && getAllDbTable) {
//                     MongodbTargetTask.setIsStopFlagOfTarget(programInfo.getProName(), true);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//
//}
