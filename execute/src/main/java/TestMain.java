import common.thread.SourceTaskPoolManager;
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
        for (int i = 0; i < Configuration.targetThreadNum; i++) {
            SourceTaskPoolManager.submit(new TargetTask(Configuration.targetName));
        }
        MongodbSource source=new MongodbSource();
        MongodbSource.syncModeOfAll();
    }
}
