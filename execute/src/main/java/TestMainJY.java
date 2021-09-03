import cache.MemoryCache;
import common.photonV.entity.ProgramInfo;
import programInfo.ProgramInfoUtil;
import datasource.DataSourceUtil;
import dbconnection.mongodb.MongoDbConnection;
import dbconnection.mysql.MySqlConnection;
import execute.MongodbTarget;
import execute.MysqlSource;
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
public class TestMainJY {
    public static void main(String[] args) throws InterruptedException {

        testMysqlToMongoDb();
    }

    public static void testMysqlToMongoDb() {
        //获取配置
        ProgramInfo programInfo = ProgramInfoUtil.getProgramInfo("proc2");
//        programInfo.setDbTableWhite("community.sys_user.*");
//        programInfo.setDbTableWhite("community.sys_menu");
//        programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)");
//        programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.community.banner)");
//        programInfo.setDbTableWhite("community.sys_user_token");
//        programInfo.setDbTableWhite("community.sys_captcha");
//        programInfo.setDbTableWhite("community.test1");
//        programInfo.setDbTableWhite("community.sys_captcha");
//        programInfo.setAdviceNumber(4);
//        programInfo.setDbTableWhite("(community.community_dict)||(community.sys_captcha)");
//        programInfo.setDbTableWhite("test.test");
        programInfo.setDbTableWhite("community.test4");
//        programInfo.setDbTableWhite("community.community_banner");
//        programInfo.setDbTableWhite("community.test");
//        programInfo.setSplitPk("banner_link");
        programInfo.setAdviceNumber(10);
//        programInfo.setSplitPk("id");
//        programInfo.setDbTableWhite("community.sys_.*");
//        programInfo.setAdviceNumber(5);

//        programInfo.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.sys_user_token)");
//        programInfo.setAdviceNumber(5);
//        programInfo.setSplitPk("dict_id");
//        programInfo.setDbTableWhite("\\w.+");
        //mysql 源连接
        MySqlConnection.createConnection(programInfo.getSourceDsName(),
                DataSourceUtil.getDataSourceByDsName(programInfo.getSourceDsName()));
        //mongodb 目标连接
        MongoDbConnection.createMonoDbClient(programInfo.getTargetDsName(),
                DataSourceUtil.getDataSourceByDsName(programInfo.getTargetDsName()));
        //缓存
        MemoryCache memoryCache = new MemoryCache(programInfo.getTaskName(),
                programInfo.getProName(), programInfo.getCacheNum(), programInfo.getCacheSize(), true);
        //配置缓存
        programInfo.setMemoryCache(memoryCache);
        //源线程池
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(programInfo.getProName(),
                programInfo.getSourceThreadNum(), programInfo.getSourceThreadNum());
        //系统线程池
        SysPoolManager sysPoolManager = new SysPoolManager(programInfo.getProName(), 5, 5);
        //目标线程池
        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(programInfo.getProName(),
                5, 5);

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
