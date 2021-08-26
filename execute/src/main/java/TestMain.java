import cache.MemoryCache;
import conf.Configuration;
import execute.MongodbSource;
import execute.MongodbTarget;
import execute.MysqlTargetLhp;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import thread.TargetTaskPoolManager;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMain {
    public static void main(String[] args) {
        testMongoDbToMysql();
    }

    //
    public static void testMongoDbToMongoDb() {

        Configuration configuration = new Configuration();
        configuration.setTargetName("1");
        configuration.setProName("1");
        configuration.setSourceName("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        configuration.setTargetName("mongodb://root:123456@192.168.3.100:6004/admin?authSource=admin");
        configuration.setSyncMode("all");
        configuration.setDbTableWhite("photon.+");
        configuration.setFilterDdl(false);
        configuration.setCollectionExistDrop(true);
        configuration.setCreateIndex(true);
        configuration.setTargetThreadNum(5);
        configuration.setSourceThreadNum(2);
        configuration.setCacheNum(20);
        configuration.setCacheSize(20);
        configuration.setDataBatchSize(128);
        configuration.setSyncParallel(false);
        configuration.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        configuration.setIncrementParseThreadNum(5);
        MemoryCache memoryCache = new MemoryCache("1", "1", 20, 20, true);
        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager("1", 2, 2);
        SourceTaskPoolManager.addSourceTaskPoolManager("1", sourceTaskPoolManager);

        SysPoolManager sysPoolManager = new SysPoolManager("1", 2, 3);
        SysPoolManager.addSysTaskPoolManager("1", sysPoolManager);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager("1", 5, 5);
        TargetTaskPoolManager.addTargetTaskPoolManager("1", targetTaskPoolManager);


        MongodbSource mongodbSource = new MongodbSource(configuration,memoryCache);
        mongodbSource.createTask();


        MongodbTarget mongodbTarget = new MongodbTarget(configuration, memoryCache, "1");
        mongodbTarget.startToTarget();

    }
    public static void testMongoDbToMysql() {

        Configuration configuration = new Configuration();
        configuration.setTargetName("1");
        configuration.setProName("1");
        configuration.setSourceName("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        configuration.setTargetName("jdbc:mysql://192.168.3.19:3306/photon?useUnicode=true&characterEncoding=utf-8");
        configuration.setSyncMode("all");
        configuration.setDbTableWhite("photon.+");
        configuration.setFilterDdl(false);
        configuration.setCollectionExistDrop(true);
        configuration.setCreateIndex(true);
        configuration.setTargetThreadNum(5);
        configuration.setSourceThreadNum(2);
        configuration.setCacheNum(20);
        configuration.setCacheSize(20);
        configuration.setDataBatchSize(128);
        configuration.setSyncParallel(false);
        configuration.setStartIncrementTime((int) (System.currentTimeMillis() / 1000));
        configuration.setIncrementParseThreadNum(5);
        MemoryCache memoryCache = new MemoryCache("1", "1", 20, 20, true);
        configuration.setMemoryCache(memoryCache);

        SourceTaskPoolManager sourceTaskPoolManager = new SourceTaskPoolManager("1", 2, 2);
        SourceTaskPoolManager.addSourceTaskPoolManager("1", sourceTaskPoolManager);

        SysPoolManager sysPoolManager = new SysPoolManager("1", 2, 3);
        SysPoolManager.addSysTaskPoolManager("1", sysPoolManager);

        TargetTaskPoolManager targetTaskPoolManager = new TargetTaskPoolManager("1", 5, 5);
        TargetTaskPoolManager.addTargetTaskPoolManager("1", targetTaskPoolManager);


        MongodbSource mongodbSource = new MongodbSource(configuration,memoryCache);
        mongodbSource.createTask();


        MysqlTargetLhp mysqlTargetLhp = new MysqlTargetLhp(configuration, memoryCache, "1");
        mysqlTargetLhp.startToTarget();

    }
}
