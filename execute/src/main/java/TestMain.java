import main.MongodbTarget;
import thread.TargetTaskPoolManager;
import conf.Configuration;
import main.MongodbSource;

import task.TargetTask;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 3:29 下午
 */
public class TestMain {
    public static void main(String[] args) {
        MongodbTarget.startToTarget();
        MongodbSource.syncModeOfAll();
    }
}
