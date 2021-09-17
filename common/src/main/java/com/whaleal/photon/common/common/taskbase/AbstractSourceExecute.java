package com.whaleal.photon.common.common.taskbase;

import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @description: sourceExe
 * @author: lhp
 * @time: 2021/7/31 1:34 下午
 */
public abstract class AbstractSourceExecute extends AbstractPhotonObject {
    /**
     * 配置文件类
     */
    protected ProgramInfo programInfo;
    /**
     * 数据缓存类
     */
    protected MemoryCache memoryCache;
    /**
     * 源端数据源名称
     */
    protected String sourceDsName;
    /**
     * 表名过滤的策略
     */
    protected String dbTableWhite;
    /**
     * 获取全部的表是否完成
     */
    protected volatile boolean isGetAllDbTable = false;

    public void setGetAllDbTable(boolean getAllDbTable) {
        isGetAllDbTable = getAllDbTable;
    }

    /**
     * 库表名map
     * k为库表名
     * v为库表名
     * 后续扩展使用到k
     */
    protected Map<String, String> dbTableNameMap = new ConcurrentHashMap<>();
    /**
     * TaskMetadata队列
     */
    protected Queue<SourceTaskInfo> taskMetadataQueue = new ConcurrentLinkedQueue<>();

    /**
     * 库表和对应的MongoNamespace
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

    private void setTaskMetadataQueue(Queue<SourceTaskInfo> taskMetadataQueue) {
        this.taskMetadataQueue = taskMetadataQueue;
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
        this.programInfo=programInfo;
        this.memoryCache = memoryCache;
        this.sourceDsName = programInfo.getSourceDsName();
        this.dbTableWhite = programInfo.getDbTableWhite();
        this.isUseDeFaultType = programInfo.isUseDeFaultType();
    }

    /**
     * getProcNameAndBatchNo
     *
     * @desc proName + batchNo
     */
    public String getProcNameAndBatchNo() {
        return proName + batchNo;
    }

    /**
     * getProcNameAndBatchNoAndSourceDsName
     *
     * @desc proName + batchNo + sourceDsName
     */
    public String getProcNameAndBatchNoAndSourceDsName() {
        return proName + batchNo + sourceDsName;
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
     * start
     *
     * @desc 全量任务
     */
    public abstract void executeQueryTask();
}
