package execute;

import common.taskbase.metadata.SourceMetadata;

import task.MysqlTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
public class MysqlTargetLhp extends SourceMetadata {


    public static void startToTarget2() {
        for (int i = 0; i < targetNum; i++) {
            TargetTaskPoolManager.submit(new MysqlTargetTask(targetName));
        }
    }


}
