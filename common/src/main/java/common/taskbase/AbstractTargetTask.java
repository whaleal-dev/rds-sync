package common.taskbase;

import cache.MemoryCache;
import common.dataclass.BatchDataEntity;
import conf.Configuration;

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
    protected String dbTableName="";
    /**
     * 任务名称
     */
    protected String taskName;
    /**
     * 程序名称
     */
    protected String proName;

    protected MemoryCache memoryCache;

    public AbstractTargetTask(Configuration configuration, MemoryCache memoryCache) {
        this.targetDsName = configuration.getTargetDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.memoryCache = memoryCache;
    }


    /**
     * applyData 应用数据
     *
     * @desc 应用数据
     */
    public abstract void applyData();

    /**
     * parseColumnDataToDocument 解析数据
     *
     * @param batchDataEntity
     * @desc 解析数据
     */
    public abstract void parseColumnDataToDocument(BatchDataEntity batchDataEntity);

    /**
     * bulkExecute 批量写数据
     *
     * @desc 批量写数据
     */

    public abstract void bulkExecute(String dbTable, long batchNo);
}
