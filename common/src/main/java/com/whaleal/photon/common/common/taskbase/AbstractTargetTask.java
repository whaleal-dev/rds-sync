package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Target 写任务基类。
 */
public abstract class AbstractTargetTask extends AbstractPhotonObject implements Runnable {
    protected String targetDsName;
    protected String dbTableName = "";
    protected MemoryCache memoryCache;
    protected boolean isUseDeFaultType;
    protected boolean isRdbOfSource = false;

    /** k = proName+batchNo */
    private static final Map<String, AtomicBoolean> IS_STOP = new ConcurrentHashMap<String, AtomicBoolean>();

    public String getProcNameAndBatchNo() {
        return proName + batchNo;
    }

    public String getProcNameAndBatchNoAndTargetDsName() {
        return proName + batchNo + targetDsName;
    }

    public AbstractTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo.getTaskName(), programInfo.getProName(), programInfo.getBatchNo());
        this.isUseDeFaultType = programInfo.isUseDeFaultType();
        this.targetDsName = programInfo.getTargetDsName();
        this.memoryCache = memoryCache;
        this.isRdbOfSource = programInfo.isRdbOfSource();
        String key = getProcNameAndBatchNo();
        if (!IS_STOP.containsKey(key)) {
            synchronized (AbstractTargetTask.class) {
                if (!IS_STOP.containsKey(key)) {
                    IS_STOP.put(key, new AtomicBoolean(false));
                }
            }
        }
    }

    public static void setIsStopFlagOfTarget(String procNameAndBatchNo, boolean value) {
        AtomicBoolean flag = IS_STOP.get(procNameAndBatchNo);
        if (flag != null) {
            flag.set(value);
        }
    }

    public static void removeIsStopFlagOfTarget(String procNameAndBatchNo) {
        IS_STOP.remove(procNameAndBatchNo);
    }

    public static boolean getIsStopFlagOfTarget(String procNameAndBatchNo) {
        AtomicBoolean flag = IS_STOP.get(procNameAndBatchNo);
        return flag != null && flag.get();
    }

    public abstract void applyData();

    public abstract void parseColumnDataToTargetData(BatchDataEntity batchDataEntity);

    public abstract void bulkExecute(String dbTable, long batchNo);
}
