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
public abstract class Metadata {
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
     * 每个数据源的开始和结束时间
     */
    protected static Map<String, List<Long>> dsTimeRange = new HashMap<>();
    /**
     * TaskMetadata队列
     */
    protected static Queue<SourceTaskMetadata> taskMetadataQueue = new ConcurrentLinkedQueue<>();

}
