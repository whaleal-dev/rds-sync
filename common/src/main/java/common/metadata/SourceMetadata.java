package common.metadata;

import com.mongodb.MongoNamespace;
import conf.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @description: MongoT的启动类的参数
 * @author: lhp
 * @time: 2021/7/31 1:34 下午
 */
public abstract class SourceMetadata {
    /**
     * 全量数据是否完成
     */
    public static volatile boolean isOver = false;
    /**
     * 目标数据源名称
     */
    protected static final String targetName = Configuration.targetName;
    /**
     * 目标数据源名称
     */
    protected static final int targetNum = Configuration.targetThreadNum;
    /**
     * 库表和对应的MongoNamespace
     */
    protected static Map<String, String> dbTables = new HashMap<>();
    /**
     * 表名过滤的策略
     */
    protected static final String dbTableWhite = Configuration.dbTableWhite;
    /**
     * TaskMetadata队列
     */
    protected static Queue<SourceTaskMetadata> taskMetadataQueue = new ConcurrentLinkedQueue<>();
    /**
     * syncModeOfAll
     *
     * @desc 全量任务
     */
    public abstract void syncModeOfAll();
    /**
     * getAllDbCollections 获取数据源中所有的库表名
     *
     * @param sourceName 数据源名称
     * @desc 获取数据源中所有的库表名
     */
    public abstract void getAllDbCollections(String sourceName);
    /**
     * startFromSource 把所有库表的中数据进行分片和创造
     *
     * @desc 启动targetTask任务
     */
    public abstract void startFromSource(String sourceName, boolean isParallel);

    public static void pushTaskMeta(SourceTaskMetadata taskMetadata) {
        taskMetadataQueue.add(taskMetadata);
    }
    /**
     * createSourceEntity 获取这个数据源的某表的且分数据
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void createSourceEntity(String sourceName, String dbTableName);
    /**
     * submitSourceTask 取task到线程池
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void  submitSourceTask();
}
