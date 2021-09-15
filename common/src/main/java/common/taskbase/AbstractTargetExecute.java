package common.taskbase;

import cache.MemoryCache;
import common.photonV.entity.ProgramInfo;
import lombok.AllArgsConstructor;

import java.util.Set;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 12:41 下午
 */
@AllArgsConstructor
public abstract class AbstractTargetExecute {

    protected ProgramInfo programInfo;

    protected MemoryCache memoryCache;

    protected String procName;
    /**
     * 批次号
     */
    protected long batchNo;

    protected String procNameAndBatchNo;

    protected String procNameAndBatchNoAndTargetDsName;

    public AbstractTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache, String procName) {
        this.programInfo = programInfo;
        this.memoryCache = memoryCache;
        this.procName = procName;
        this.batchNo = programInfo.getBatchNO();
        this.procNameAndBatchNo = procName + batchNo;
        this.procNameAndBatchNoAndTargetDsName = procName + batchNo + programInfo.getTargetDsName();
    }


    /**
     * 开始启动任务
     */
    public abstract void startToTarget();

    /**
     * @param dbTableNameSet
     */
    public abstract void rollBackDataFromDbTable(Set<String> dbTableNameSet);

    public abstract void deleteExistDbTable(Set<String> dbTableNameSet);
}
