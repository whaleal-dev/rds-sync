package execute;

import cache.MemoryCache;

import common.photonV.entity.ProgramInfo;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import task.MysqlTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
@AllArgsConstructor
@NoArgsConstructor
public class MysqlTarget {
    private ProgramInfo programInfo;
    private MemoryCache memoryCache;
    private String procName;

    public void startToTarget() {
        for (int i = 0; i < 10; i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(procName, 1);
            TargetTaskPoolManager.submit(procName, new MysqlTargetTask(programInfo, memoryCache));
        }
    }
}
