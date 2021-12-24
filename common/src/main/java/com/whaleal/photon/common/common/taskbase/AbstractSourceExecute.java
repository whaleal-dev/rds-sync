package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: sourceExe
 * @author: lhp
 * @time: 2021/7/31 1:34 下午
 */
public abstract class AbstractSourceExecute extends AbstractPhotonObject {
    /**
     * 配置文件类
     */
    protected final ProgramInfo programInfo;
    /**
     * 数据缓存类
     */
    protected final MemoryCache memoryCache;
    /**
     * 源端数据源名称
     */
    protected final String sourceDsName;
    /**
     * 表名过滤的策略
     */
    protected final String dbTableWhite;
    /**
     * 获取全部的表是否完成
     */
    protected volatile boolean isGetAllDbTable = false;

    protected final AtomicInteger maxSourceTaskInfoNum = new AtomicInteger();


    public int getMaxSourceTaskInfoNum() {
        return maxSourceTaskInfoNum.get();
    }

    /**
     * TaskMetadata队列
     */
    protected final Queue<SourceTaskInfo> taskMetadataQueue = new ConcurrentLinkedQueue<>();
    /**
     * 库表
     */
    protected Set<String> dbTableNameSet = new HashSet<>();
    /**
     * 判断源和目标是否为同一类数据源
     */
    protected boolean isUseDeFaultType = false;

    /**
     * isGetAllDbTable
     *
     * @desc 获取全部的表是否完成
     */
    public boolean isGetAllDbTable() {
        return isGetAllDbTable;
    }

    /**
     * getTaskMetadataQueueSize 获取未执行TaskInfo的个数
     *
     * @desc 获取未执行TaskInfo的个数
     */
    public int getTaskMetadataQueueSize() {
        return taskMetadataQueue.size();
    }

    /**
     * getDbTableNameSet 获取同步的表单
     *
     * @desc 获取同步的表单
     */
    public Set<String> getDbTableNameSet() {
        return dbTableNameSet;
    }

    public AbstractSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo.getTaskName(), programInfo.getProName(), programInfo.getBatchNo());
        this.programInfo = programInfo;
        this.memoryCache = memoryCache;
        this.sourceDsName = programInfo.getSourceDsName();
        this.dbTableWhite = programInfo.getDbTableWhite();
        this.isUseDeFaultType = programInfo.isUseDeFaultType();
    }


    /**
     * start
     *
     * @desc 全量任务
     */
    public abstract void start();

    /**
     * getAllDbTables 获取数据源中所有的库表名
     *
     * @desc 获取数据源中所有的库表名
     */
    public abstract void getAllDbTables();

    /**
     * splitDbTable 把所有库表的中数据进行分片和创造
     *
     * @desc 启动targetTask任务
     */
    public abstract void splitDbTable();

    /**
     * createSourceTask 获取这个数据源的某表的且分数据
     *
     * @param dbTableName 库表名
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void createSourceTask(String dbTableName);

    /**
     * submitSourceTask 取task到线程池
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void submitSourceTask();

    /**
     * executeQueryTask
     *
     * @desc 全量任务
     */
    public abstract void executeQueryTask();
}
