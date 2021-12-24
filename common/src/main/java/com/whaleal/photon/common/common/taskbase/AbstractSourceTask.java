package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 5:27 下午
 */
public abstract class AbstractSourceTask  extends AbstractPhotonObject implements Runnable {
    /**
     * 缓存对象
     */
    protected MemoryCache memoryCache;
    /**
     * 任务配置信息
     */
    protected SourceTaskInfo taskMetadata;
    /**
     * 源数据源名称
     */
    protected String sourceDsName;
    /**
     * 缓存大小
     */
    protected long cache = 0L;
    /**
     * 每个批次数据的大小
     */
    protected int dataBatchSize = 128;
    /**
     * 两边数据源是否一致
     */
    protected boolean isUserDeFaultType=false;
    /**
     * 缓存数据集合
     */
    protected List<List<AbstractColumn>> dataList = new ArrayList<>();
    /**
     * 用于计算现在这批数据量的大小
     */
    protected int cacheTemp = 0;

    protected int writeNum = 0;

    protected boolean scanOver = false;

    public AbstractSourceTask(SourceTaskInfo taskMetadata,ProgramInfo programInfo) {

        super(programInfo.getTaskName(),programInfo.getProName(),programInfo.getBatchNo());

        this.sourceDsName = taskMetadata.getSourceDsName();
        this.memoryCache = programInfo.getMemoryCache();
        this.dataBatchSize = programInfo.getDataBatchSize();
        this.isUserDeFaultType = programInfo.isUseDeFaultType();
        this.taskMetadata = taskMetadata;

    }
    /**
     * getDataFromDbTable 获取表数据
     *
     * @desc 获取表数据
     */
    public abstract void getDataFromDbTable();

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
     * @desc 解析数据
     */
    public abstract void dataTransformation(Object object);
}
