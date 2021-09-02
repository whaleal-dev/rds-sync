package execute;

import cache.MemoryCache;
import conf.ProgramInfo;
import lombok.AllArgsConstructor;
import task.MongodbTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
@AllArgsConstructor
public class MongodbTarget {
    private ProgramInfo programInfo;
    private MemoryCache memoryCache;
    private String procName;

    public void startToTarget() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procName, 1);
            //System.out.println("setTargetActiveThreadNum" + TargetTaskPoolManager.setTargetActiveThreadNum(procName, 0));
            TargetTaskPoolManager.submit(procName, new MongodbTargetTask(programInfo, memoryCache));
        }

    }
}
