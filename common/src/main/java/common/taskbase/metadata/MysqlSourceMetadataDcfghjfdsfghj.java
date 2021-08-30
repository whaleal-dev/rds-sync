//package common.taskbase.metadata;
//
//import cache.MemoryCache;
//
//import common.taskbase.SourceTaskInfo;
//import conf.Configuration;
//
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Queue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
///**
// * mysql source
// *
// * @author: jy
// * @Date: 2021/08/27
// */
//public abstract class MysqlSourceMetadataDcfghjfdsfghj {
//
//    /**
//     * 配置文件类
//     */
//    protected Configuration configuration;
//
//    protected MemoryCache memoryCache;
//
//    /**
//     * 源端数据源名称
//     */
//    protected String sourceName;
//    /**
//     * 任务名称
//     */
//    protected String taskName;
//    /**
//     * 程序名称
//     */
//    protected String proName;
//    /**
//     * 表名过滤的策略
//     */
//    protected String dbTableWhite;
//    /**
//     * 获取全部的表是否完成
//     */
//    protected volatile boolean isGetAllDbTable = false;
//
//    public boolean isGetAllDbTable() {
//        return isGetAllDbTable;
//    }
//
//    public int getTaskMetadataQueueSize() {
//        return mysqlTaskMetadataQueue.size();
//    }
//
//    public void setTaskMetadataQueue(Queue<MysqlSourceTaskInfo> mysqlTaskMetadataQueue) {
//        this.mysqlTaskMetadataQueue = mysqlTaskMetadataQueue;
//    }
//    /**
//     * 库表和对应的MongoNamespace
//     */
//    protected Map<String, String> dbTables = new ConcurrentHashMap<>();
//    /**
//     * TaskMetadata队列
//     */
//
//    protected static Queue<MysqlSourceTaskInfo> mysqlTaskMetadataQueue = new ConcurrentLinkedQueue<>();
//
//    /**
//     * createTask
//     *
//     * @desc 全量任务
//     */
//    public abstract void createTask();
//
//    /**
//     * getAllDbTables 获取数据源中所有的库表名
//     *
//     * @param sourceName 数据源名称
//     * @desc 获取数据源中所有的库表名
//     */
//    public void getAllDbTables(String sourceName) throws SQLException {}
//
//    /**
//     * startFromSource 把所有库表的中数据进行分片和创造
//     *
//     * @param sourceName 数据源名称
//     * @param isParallel 是否并行
//     * @desc 启动targetTask任务
//     */
//    public abstract void startFromSource(String sourceName, boolean isParallel);
//
//    /**
//     * createSourceEntity 获取这个数据源的某表的且分数据
//     *
//     * @param configuration  数据源配置
//     * @param dbTableName 库表名
//     * @desc 获取这个数据源的某表的且分数据
//     */
//    public abstract void createSourceEntity(Configuration configuration, String dbTableName);
//
//    /**
//     * submitSourceTask 取task到线程池
//     *
//     * @desc 获取这个数据源的某表的且分数据
//     */
//    public abstract void submitSourceTask();
//
//}
