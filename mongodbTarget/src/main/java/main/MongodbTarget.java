package main;

import common.metadata.Metadata;
import manger.thread.TaskPoolManager;

import task.TargetTask;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
public class MongodbTarget extends Metadata {



    public static void main(String[] args) {

    }

    public static void startToTarget() {
        for (int i = 0; i < targetNum; i++) {
            TaskPoolManager.submit(new TargetTask(targetName));
        }
    }



}
