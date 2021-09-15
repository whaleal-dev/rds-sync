package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.Set;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 12:41 下午
 */

public abstract class AbstractTargetExecute extends AbstractPhotonObject {

    protected ProgramInfo programInfo;

    protected MemoryCache memoryCache;

    public String getProcNameAndBatchNo() {
        return proName + batchNo;
    }

    public String getProcNameAndBatchNoAndTargetDsName() {
        return proName + batchNo + programInfo.getTargetDsName();
    }

    public AbstractTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo.getTaskName(), programInfo.getProName(), programInfo.getBatchNo());
        this.programInfo = programInfo;
        this.memoryCache = memoryCache;
    }

    /**
     * 开始启动任务
     */
    public abstract void start();

    /**
     * 数据回退
     *
     * @param dbTableNameSet
     */
    public abstract void rollBackDataFromDbTable(Set<String> dbTableNameSet);

    /**
     * 删除已经存在的同步表
     *
     * @param dbTableNameSet
     */
    public abstract void deleteExistDbTable(Set<String> dbTableNameSet);
}
