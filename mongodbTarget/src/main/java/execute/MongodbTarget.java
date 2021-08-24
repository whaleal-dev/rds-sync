package execute;

import conf.Configuration;
import task.MongodbTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
public class MongodbTarget {

    public static void startToTarget() {
        for (int i = 0; i < Configuration.targetThreadNum; i++) {
            TargetTaskPoolManager.submit(new MongodbTargetTask(Configuration.targetName));
        }
    }
}
