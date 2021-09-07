package common.taskbase;

import cache.MemoryCache;
import common.photonV.entity.ProgramInfo;
import lombok.AllArgsConstructor;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 12:41 下午
 */
@AllArgsConstructor
public abstract class AbstractTarget {
    protected ProgramInfo programInfo;
    protected MemoryCache memoryCache;
    protected String procName;
    /**
     * 批次号
     */
    protected long batchNo;

    public AbstractTarget(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        this.programInfo = programInfo;
        this.memoryCache = memoryCache;
        this.procName = procName;
    }


    /**
     * 开始启动任务
     */
    public abstract void startToTarget();
}
