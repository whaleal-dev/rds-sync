package common.taskbase.metadata;

import cache.MemoryCache;
import common.taskbase.SourceTaskInfo;
import common.photonV.entity.ProgramInfo;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @description: PhotonT的启动类的参数
 * @author: lhp
 * @time: 2021/7/31 1:34 下午
 */
public abstract class SourceMetadata {
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
     * 任务名称
     */
    protected String taskName;
    /**
     * 程序名称
     */
    protected String proName;
    /**
     * 批次号
     */
    protected long batchNo;
    /**
     * 表名过滤的策略
     */
    protected String dbTableWhite;
    /**
     * 获取全部的表是否完成
     */
    protected volatile boolean isGetAllDbTable = false;
    /**
     * 库表和对应的MongoNamespace
     */
    protected Map<String, String> dbTables = new ConcurrentHashMap<>();
    /**
     * TaskMetadata队列
     */
    protected Queue<SourceTaskInfo> taskMetadataQueue = new ConcurrentLinkedQueue<>();

    protected String procNameAndBatchNo;

    protected String procNameAndBatchNoAndSourceDsName;

    /**
     * 库表和对应的MongoNamespace
     */
    protected Set<String> dbTableNameSet = new HashSet<>();

    protected boolean isUseDeFaultType = false;

    /**
     * getTaskMetadataQueueSize 获取未执行TaskInfo的个数
     *
     * @desc 获取未执行TaskInfo的个数
     */
    public int getTaskMetadataQueueSize() {
        return taskMetadataQueue.size();
    }

    public boolean isGetAllDbTable() {
        return isGetAllDbTable;
    }

    public void setTaskMetadataQueue(Queue<SourceTaskInfo> taskMetadataQueue) {
        this.taskMetadataQueue = taskMetadataQueue;
    }

    public Set<String> getDbTableNameSet() {
        return dbTableNameSet;
    }

    public SourceMetadata(ProgramInfo programInfo, MemoryCache memoryCache) {
        this.sourceDsName = programInfo.getSourceDsName();
        this.taskName = programInfo.getTaskName();
        this.proName = programInfo.getProName();
        this.dbTableWhite = programInfo.getDbTableWhite();
        this.memoryCache = memoryCache;
        this.batchNo = programInfo.getBatchNO();
        this.procNameAndBatchNo = proName + batchNo;
        this.procNameAndBatchNoAndSourceDsName = proName + batchNo + sourceDsName;
        this.isUseDeFaultType = programInfo.isUseDeFaultType();
    }

    /**
     * createTask
     *
     * @desc 全量任务
     */
    public abstract void createTask();

    /**
     * getAllDbCollections 获取数据源中所有的库表名
     *
     * @param sourceName 数据源名称
     * @desc 获取数据源中所有的库表名
     */
    public abstract void getAllDbCollections(String sourceName) throws SQLException;

    /**
     * startFromSource 把所有库表的中数据进行分片和创造
     *
     * @param sourceName 数据源名称
     * @param isParallel 是否并行
     * @desc 启动targetTask任务
     */
    public abstract void startFromSource(String sourceName, boolean isParallel);

    /**
     * createSourceEntity 获取这个数据源的某表的且分数据
     *
     * @param sourceName  数据源名称
     * @param dbTableName 库表名
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void createSourceEntity(String sourceName, String dbTableName);

    /**
     * submitSourceTask 取task到线程池
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void submitSourceTask();
}
