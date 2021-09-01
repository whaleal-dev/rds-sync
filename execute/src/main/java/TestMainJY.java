import cache.MemoryCache;
import common.OplogMetadata;
import common.photonV.entity.TaskTrigger;
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
import trigger.TriggerUtil;
import util.Log;
import util.StringUtil;


/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMainJY {
    public static void main(String[] args) throws InterruptedException {

//        Configuration configuration = ConfigurationUtil.getConfiguration("proc3");
//        MongoClient mongoClient = MongoDbConnection.getMongoClient(configuration.getSourceDsName(), DataSourceUtil.getDataSourceByDsName(configuration.getProName(), configuration.getSourceDsName()));
//        MongoCursor<String> photon = mongoClient.getDatabase("photon").listCollectionNames().iterator();
//        while (photon.hasNext()){
//            String s = photon.next().toString();
//            System.out.println(s+"   "+mongoClient.getDatabase("photon").getCollection(s).countDocuments());
//        }
//        Thread.sleep(10000);
//        testMongoDbToMongoDb();
        testMysqlToMongoDb();
    }

    public static void testMysqlToMongoDb() {
        //获取配置
        Configuration configuration = ConfigurationUtil.getConfiguration("proc2");
//        configuration.setDbTableWhite("community.sys_user.*");
//        configuration.setDbTableWhite("community.sys_menu");
//        configuration.setDbTableWhite("(community.community_dict)||(community.sys_menu)");
        configuration.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.community.banner)");
//        configuration.setDbTableWhite("community.sys_user_token");
//        configuration.setDbTableWhite("community.sys_captcha");
//        configuration.setAdviceNumber(4);
//        configuration.setDbTableWhite("(community.community_dict)||(community.sys_captcha)");
//        configuration.setDbTableWhite("community.+");
//        configuration.setDbTableWhite("community.community_banner");
//        configuration.setDbTableWhite("community.test");
//        configuration.setSplitPk("banner_link");
        configuration.setAdviceNumber(3);
//        configuration.setDbTableWhite("community.sys_.*");
//        configuration.setAdviceNumber(5);

//        configuration.setDbTableWhite("(community.community_dict)||(community.sys_menu)||(community.sys_user_token)");
//        configuration.setAdviceNumber(5);
//        configuration.setSplitPk("dict_id");
//        configuration.setDbTableWhite("\\w.+");
        //mysql 源连接
        MySqlConnection.createConnection(configuration.getSourceDsName(),
                DataSourceUtil.getDataSourceByDsName(configuration.getSourceDsName()));
        //mongodb 目标连接
        MongoDbConnection.createMonoDbClient(configuration.getTargetDsName(),
                DataSourceUtil.getDataSourceByDsName(configuration.getTargetDsName()));
        //缓存
        MemoryCache memoryCache = new MemoryCache(configuration.getTaskName(),
                configuration.getProName(), configuration.getCacheNum(), configuration.getCacheSize(), true);
        //配置缓存
        configuration.setMemoryCache(memoryCache);
        //源线程池
//        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(),
//                3, 3);
        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager(configuration.getProName(),
                configuration.getSourceThreadNum(), configuration.getSourceThreadNum());
//        SourceTaskPoolManager.addSourceTaskPoolManager(configuration.getProName(), sourceTaskPoolManager);
        //系统线程池
        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(), 5, 5);
//        SysPoolManager sysPoolManager = new SysPoolManager(configuration.getProName(),
//                configuration.getSourceThreadNum(), configuration.getSourceThreadNum());
//        SysPoolManager.addSysTaskPoolManager(configuration.getProName(), sysPoolManager);
        //目标线程池
        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager(configuration.getProName(),
                5, 5);
//        TargetTaskPoolManager.addTargetTaskPoolManager(configuration.getProName(), targetTaskPoolManager);

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
