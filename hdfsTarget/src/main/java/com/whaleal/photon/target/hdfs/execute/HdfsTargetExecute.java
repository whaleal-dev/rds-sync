package com.whaleal.photon.target.hdfs.execute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractTargetExecute;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.target.hdfs.task.HdfsTargetTask;

import java.util.Set;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/22 4:26 下午
 */
public class HdfsTargetExecute extends AbstractTargetExecute {
    public HdfsTargetExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
    }

    @Override
    public void start() {
        for (int i = 0; i < programInfo.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), 1);
            TargetTaskPoolManager.submit(getProcNameAndBatchNo(), new HdfsTargetTask(programInfo, memoryCache));
        }
    }

    @Override
    public void rollBackDataFromDbTable(Set<String> dbTableNameSet) {

    }

    @Override
    public void deleteExistDbTable(Set<String> dbTableNameSet) {

    }

    @Override
    public void executePreSql(String sql) {

    }
}
