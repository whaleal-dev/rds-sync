package main;

import common.metadata.Metadata;

import task.TargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
public class MongodbTarget extends Metadata {


    public static void startToTarget() {
        for (int i = 0; i < targetNum; i++) {
            TargetTaskPoolManager.submit(new TargetTask(targetName));
        }
    }


}
