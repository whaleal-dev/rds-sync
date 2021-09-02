package execute;

import cache.MemoryCache;

import common.photonV.entity.ProgramInfo;
import common.taskbase.AbstractTarget;
import task.MysqlTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: MysqlTarget
 */

public class MysqlTarget extends AbstractTarget {

    public MysqlTarget(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        super(programInfo, memoryCache, procName);
    }

    @Override
    public void startToTarget() {
        for (int i = 0; i < 10; i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procName, 1);
            TargetTaskPoolManager.submit(procName, new MysqlTargetTask(programInfo, memoryCache));
        }
    }
}
