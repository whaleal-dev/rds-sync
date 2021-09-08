package common.taskbase;

import cache.MemoryCache;
import common.dataclass.BatchDataEntity;
import common.photonV.entity.ProgramInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 5:27 下午
 */
public abstract class AbstractTargetTask implements Runnable {
    /**
     * 目标数据源名称
     */
    protected String targetDsName;
    /**
     * dbTableName
     */
    protected String dbTableName = "";
    /**
     * 任务名称
     */
    protected String taskName;
    /**
     * 程序名称
     */
    protected String proName;
    /**
     *
     */
    protected long batchNO;
    /**
     * 数据缓存类
     */
    protected MemoryCache memoryCache;

    protected String procNameAndBatchNo;

    protected String procNameAndBatchNoAndTargetDsName;

    public AbstractTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        this.targetDsName = programInfo.getTargetDsName();
        this.taskName = programInfo.getTaskName();
        this.proName = programInfo.getProName();
        this.memoryCache = memoryCache;
        this.batchNO = programInfo.getBatchNO();
        this.procNameAndBatchNo = proName + batchNO;
        this.procNameAndBatchNoAndTargetDsName = proName + batchNO + targetDsName;
        if (!isStop.containsKey(proName + batchNO)) {
            synchronized (AbstractTargetTask.class) {
                if (!isStop.containsKey(proName + batchNO)) {
                    isStop.put(proName + batchNO, new AtomicBoolean());
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
     * 该target是否停止
     */
    private volatile static Map<String, AtomicBoolean> isStop = new ConcurrentHashMap<>();

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
