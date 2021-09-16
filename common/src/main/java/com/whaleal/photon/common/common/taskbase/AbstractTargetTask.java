package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 5:27 下午
 */
public abstract class AbstractTargetTask extends AbstractPhotonObject implements Runnable {
    /**
     * 目标数据源名称
     */
    protected String targetDsName;
    /**
     * dbTableName
     */
    protected String dbTableName = "";
    /**
     * 数据缓存类
     */
    protected MemoryCache memoryCache;
    /**
     * 某target是否停止
     * k为程序名+批次号
     * v为boolean
     */
    private volatile static Map<String, AtomicBoolean> isStop = new ConcurrentHashMap<>();
    /**
     * 两端是否为同一类数据源
     */
    protected boolean isUseDeFaultType;
    /**
     * 源端是否为rdb数据库
     */
    protected boolean isRdbOfSource = false;

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
        if (!isStop.containsKey(getProcNameAndBatchNo())) {
            synchronized (AbstractTargetTask.class) {
                if (!isStop.containsKey(getProcNameAndBatchNo())) {
                    isStop.put(getProcNameAndBatchNo(), new AtomicBoolean());
                }
            }
        }
    }

    /**
     * 设置某pro的target是否停止
     */
    public static void setIsStopFlagOfTarget(String procNameAndBatchNo, boolean value) {
        isStop.get(procNameAndBatchNo).set(value);
    }

    public static void removeIsStopFlagOfTarget(String procNameAndBatchNo) {
        isStop.remove(procNameAndBatchNo);
    }

    public static boolean getIsStopFlagOfTarget(String procNameAndBatchNo) {
        return isStop.get(procNameAndBatchNo).get();
    }


    /**
     * applyData 应用数据
     *
     * @desc 应用数据
     */
    public abstract void applyData();

    /**
     * parseColumnDataToTargetData 解析数据
     *
     * @param batchDataEntity
     * @desc 解析数据
     */
    public abstract void parseColumnDataToTargetData(BatchDataEntity batchDataEntity);

    /**
     * bulkExecute 批量写数据
     *
     * @param dbTable
     * @param batchNo
     * @desc 批量写数据
     */
    public abstract void bulkExecute(String dbTable, long batchNo);
}
