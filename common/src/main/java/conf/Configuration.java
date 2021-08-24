package conf;

import lombok.ToString;

import java.util.Arrays;
import java.util.List;

/**
 * @author: lhp
 * @time: 2021/7/22 2:00 下午
 * @desc: 配置文件类
 */
@ToString
public class Configuration {
    /**
     * 源数据源列表
     */
    public static final String sourceName;
    /**
     * 目标数据源名称
     */
    public static final String targetName;
    /**
     * 同步模式
     */
    public static final String syncMode;
    /**
     * 表过滤正则
     */
    public static final String dbTableWhite;
    /**
     * 是否同步DDL
     */
    public static final boolean filterDdl;
    /**
     * 已经存在的表是否删除
     */
    public static final boolean collectionExistDrop;
    /**
     * 是否创建索引
     */
    public static final boolean createIndex;
    /**
     * target任务线程数
     */
    public static final int targetThreadNum;
    /**
     * source任务线程数
     */
    public static final int sourceThreadNum;
    /**
     * 缓存区个数
     */
    public static final int cacheSize;
    /**
     * 每个缓存区缓存批次数量
     */
    public static final int cacheNum;
    /**
     * 每个批次数据的大小
     */
    public static final int dataBatchSize;
    /**
     * 多个源数据源是否并行执行
     */
    //public static final boolean syncParallel;
    /**
     * #增量同步时，设置增量开始同步的时间。时间戳格式，单位s
     */
    public static final int startIncrementTime;
    /**
     * 在增量中每个数据源解析数据的线程
     */
    public static final int incrementParseThreadNum;

    static {

         sourceName = Property.getPropertiesByKey("source.data.mongodb.uri");

        /**
         * 目标数据源url
         */
        targetName = Property.getPropertiesByKey("target.data.mongodb.uri");
        /**
         * 同步模式。增量 全量 增量+全量
         */
        syncMode = Property.getPropertiesByKey("sync_mode");
        /**
         * 表过滤
         */
        dbTableWhite = Property.getPropertiesByKey("filter.namespace.white");
        /**
         * 是否同步ddl
         */
        filterDdl = Boolean.parseBoolean(Property.getPropertiesByKey("filter.ddl_enable"));
        /**
         * 已经存在的表 是否删除
         */
        collectionExistDrop = Boolean.parseBoolean(Property.getPropertiesByKey("full_sync.collection_exist_drop"));
        /**
         * 是否建立索引
         */
        createIndex = Boolean.parseBoolean(Property.getPropertiesByKey("full_sync.create_index"));
        /**
         * 写入端的线程数
         * sys=系统cpu个数*0.7
         */
        String targetNumStr = Property.getPropertiesByKey("task.target.thread.num");
        int copNum = Runtime.getRuntime().availableProcessors();
        if ("sys".equals(targetNumStr)) {
            targetThreadNum = (int) Math.round((copNum + 1) * 0.7);
        } else if (targetNumStr.length() == 0) {
            targetThreadNum = (int) Math.round((copNum + 1) * 0.7);
        } else {
            int targetNumTemp = Integer.parseInt(targetNumStr);
            targetThreadNum = targetNumTemp;
        }
        /**
         * 读取端的线程数
         * sys=系统cpu个数*0.3
         */
        String sourceNumStr = Property.getPropertiesByKey("task.source.thread.num");
        if ("sys".equals(sourceNumStr)) {
            sourceThreadNum = (int) Math.round((copNum + 1) * 0.3);
        } else if (sourceNumStr.length() == 0) {
            sourceThreadNum = (int) Math.round((copNum + 1) * 0.3);
        } else {
            int sourceNumTemp = Integer.parseInt(sourceNumStr);
            sourceThreadNum = sourceNumTemp;
        }
        /**
         * 缓存区个数
         */
        cacheSize = Integer.parseInt(Property.getPropertiesByKey("cache.size"));
        /**
         * 缓存桶个数
         */
        cacheNum = Integer.parseInt(Property.getPropertiesByKey("cache.num"));
        /**
         * 每个批次数据的大小
         */
        dataBatchSize = Integer.parseInt(Property.getPropertiesByKey("data.batch_size"));
        /**
         * #增量同步时，设置增量开始同步的时间。时间戳格式，单位s
         * now=当前任务启动的时间戳
         */
        if (("now").equalsIgnoreCase(Property.getPropertiesByKey("increment.start.time"))) {
            startIncrementTime = (int) (System.currentTimeMillis() / 1000);
        } else {
            startIncrementTime = Integer.parseInt(Property.getPropertiesByKey("increment.start.time"));
        }
        /**
         * 在增量中每个数据源解析数据的线程
         */
        incrementParseThreadNum = Integer.parseInt(Property.getPropertiesByKey("increment.parse.thread_num"));
    }

    public static void main(String[] args) {
        System.out.println("ys.test22".matches(dbTableWhite));
    }
}
