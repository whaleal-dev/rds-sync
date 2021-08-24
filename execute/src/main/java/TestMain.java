import execute.MongodbSource;
import execute.MongodbTarget;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMain {
        public static void main(String[] args) {
            testMongoDbToMongoDb();
    }
//
    public static  void testMongoDbToMongoDb() {
        MongodbSource mongodbSource = new MongodbSource();
        mongodbSource.syncModeOfAll();
       MongodbTarget.startToTarget();
    }
//
//    public void testMongoDbToMysql() {
//        MysqlTargetLhp.startToTarget2();
//        MongodbSource.syncModeOfAll();
//    }
}
