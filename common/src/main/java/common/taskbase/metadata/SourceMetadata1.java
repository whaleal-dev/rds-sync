package common.taskbase.metadata;

import cache.MemoryCache;
import conf.Configuration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @description: MongoT的启动类的参数
 * @author: lhp
 * @time: 2021/7/31 1:34 下午
 */
public abstract class SourceMetadata1 {

    /**
     * 配置文件类
     */
    protected Configuration configuration;

    protected MemoryCache memoryCache;

    /**
     * 源端数据源名称
     */
    protected String sourceName;
    /**
     * 任务名称
     */
    protected String taskName;
    /**
     * 程序名称
     */
    protected String proName;
    /**
     * 表名过滤的策略
     */
    protected String dbTableWhite;
    /**
     * 全量数据是否完成
     */
    protected volatile boolean isOver = false;
    /**
     * 库表和对应的MongoNamespace
     */
    protected static Map<String, String> dbTables = new HashMap<>();
    /**
     * TaskMetadata队列
     */

    protected static Queue<SourceTaskInfo1> taskMetadataQueue1 = new ConcurrentLinkedQueue<>();

    /**
     * createTask
     *
     * @desc 全量任务
     */
    public abstract void createTask() throws SQLException;

    /**
     * getAllDbTables 获取数据源中所有的库表名
     *
     * @param conf 数据源名称
     * @desc 获取数据源中所有的库表名
     */
    public void getAllDbTables(Configuration conf) throws SQLException {

    }

    /**
     * startFromSource 把所有库表的中数据进行分片和创造
     *
     * @param conf 数据源名称
     * @param isParallel 是否并行
     * @desc 启动targetTask任务
     */
    public abstract void startFromSource(Configuration conf, boolean isParallel);

    /**
     * createSourceEntity 获取这个数据源的某表的且分数据
     *
     * @param conf  数据源名称
     * @param dbTableName 库表名
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void createSourceEntity(Configuration conf, String dbTableName);

    /**
     * submitSourceTask 取task到线程池
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public abstract void submitSourceTask();
}
