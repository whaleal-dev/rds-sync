package execute;

import cache.MemoryCache;
import common.photonV.entity.ProgramInfo;
import common.taskbase.AbstractTargetInfo;
import lombok.AllArgsConstructor;
import task.MongodbTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */

public class MongodbTarget extends AbstractTargetInfo {
    public MongodbTarget(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        super(programInfo, memoryCache, procName);
    }
    @Override
    public void startToTarget() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procName, 1);
            TargetTaskPoolManager.submit(procName, new MongodbTargetTask(programInfo, memoryCache));
        }
    }

}
