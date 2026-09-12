package com.whaleal.rds.common.common.taskbase;

import com.whaleal.rds.common.cache.MemoryCache;
import com.whaleal.rds.common.common.dataclass.BatchDataEntity;
import com.whaleal.rds.common.common.syncerV.entity.ProgramInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sink 写任务基类。
 */
public abstract class AbstractSinkTask extends AbstractPhotonObject implements Runnable {
    protected String sinkDsName;
    protected String dbTableName = "";
    protected MemoryCache memoryCache;
    protected boolean isUseDeFaultType;
    protected boolean isRdbOfSource = false;

    /** k = proName+batchNo */
    private static final Map<String, AtomicBoolean> IS_STOP = new ConcurrentHashMap<String, AtomicBoolean>();

    public String getProcNameAndBatchNo() {
        return proName + batchNo;
    }

    public String getProcNameAndBatchNoAndSinkDsName() {
        return proName + batchNo + sinkDsName;
    }

    public AbstractSinkTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo.getTaskName(), programInfo.getProName(), programInfo.getBatchNo());
        this.isUseDeFaultType = programInfo.isUseDeFaultType();
        this.sinkDsName = programInfo.getSinkDsName();
        this.memoryCache = memoryCache;
        this.isRdbOfSource = programInfo.isRdbOfSource();
        String key = getProcNameAndBatchNo();
        if (!IS_STOP.containsKey(key)) {
            synchronized (AbstractSinkTask.class) {
                if (!IS_STOP.containsKey(key)) {
                    IS_STOP.put(key, new AtomicBoolean(false));
                }
            }
        }
    }

    public static void setIsStopFlagOfSink(String procNameAndBatchNo, boolean value) {
        AtomicBoolean flag = IS_STOP.get(procNameAndBatchNo);
        if (flag != null) {
            flag.set(value);
        }
    }

    public static void removeIsStopFlagOfSink(String procNameAndBatchNo) {
        IS_STOP.remove(procNameAndBatchNo);
    }

    public static boolean getIsStopFlagOfSink(String procNameAndBatchNo) {
        AtomicBoolean flag = IS_STOP.get(procNameAndBatchNo);
        return flag != null && flag.get();
    }

    public abstract void applyData();

    public abstract void parseColumnDataToSinkData(BatchDataEntity batchDataEntity);

    public abstract void bulkExecute(String dbTable, long batchNo);
}
