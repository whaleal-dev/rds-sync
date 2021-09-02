package common.taskbase;

import cache.MemoryCache;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.photonV.entity.ProgramInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 5:27 下午
 */
public abstract class AbstractSourceTask implements Runnable {
    /**
     * 缓存对象
     */
    protected MemoryCache memoryCache;
    /**
     * 程序名
     */
    protected String procName;
    /**
     * 任务配置信息
     */
    protected SourceTaskInfo taskMetadata;
    /**
     * 缓存大小
     */
    protected long cache = 0L;
    /**
     * 每个批次数据的大小
     */
    protected int dataBatchSize = 128;
    /**
     * 缓存数据集合
     */
    protected List<List<AbstractColumn>> dataList = new ArrayList<>();


    public AbstractSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        this.procName = procName;
        this.memoryCache = memoryCache;
        this.dataBatchSize = dataBatchSize;
        this.taskMetadata = taskMetadata;
    }

    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    public abstract void getDataFromCollection();

    /**
     * putDataToCache 放数据到缓存对象中
     *
     * @desc 放数据到缓存对象中
     */
    public abstract void putDataToCache();

    /**
     * dataTransformation 解析数据到AbstractColumn
     *
     * @param object
     * @desc 放数据到缓存对象中
     */
    public abstract void dataTransformation(Object object);
}
