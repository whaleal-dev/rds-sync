package common.photonV.entity;

import cache.MemoryCache;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * @author: lhp
 * @time: 2021/7/22 2:00 下午
 * @desc: 配置文件类
 */
@ToString
@Data
@NoArgsConstructor
public class ProgramInfo {
    private MemoryCache memoryCache;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 批次号
     */
    private long batchNO = 0L;
    /**
     * 程序名称
     */
    private String proName;
    /**
     * 源端数据源名称
     */
    private String sourceDsName;
    /**
     * 目标数据源名称
     */
    private String targetDsName;
    /**
     * 同步模式
     */
    private String syncMode;
    /**
     * 表过滤正则
     */
    private String dbTableWhite;
    /**
     * 是否同步DDL
     */
    private boolean filterDdl;
    /**
     * 已经存在的表是否删除
     */
    private boolean collectionExistDrop;
    /**
     * 是否创建索引
     */
    private boolean createIndex;
    /**
     * target任务线程数
     */
    private int targetThreadNum;
    /**
     * source任务线程数
     */
    private int sourceThreadNum;
    /**
     * 缓存区个数
     */
    private int cacheSize;
    /**
     * 每个缓存区缓存批次数量
     */
    private int cacheNum;
    /**
     * 每个批次数据的大小
     */
    private int dataBatchSize;
    /**
     * 多个源数据源是否并行执行
     */
    private boolean syncParallel;
    /**
     * #增量同步时，设置增量开始同步的时间。时间戳格式，单位s
     */
    private int startIncrementTime;
    /**
     * 在增量中每个数据源解析数据的线程
     */
    private int incrementParseThreadNum;
    /**
     * 切表字段
     */
    private String splitPk;
    /**
     * PK_TYPE
     */
    private Object PK_TYPE;
    /**
     *
     */
    private Integer fetchSize = 500;
    /**
     * 切分数
     */
    private Integer adviceNumber = 2;
    /**
     * 查询条件
     * 仅使用于查询。不可应用于库表同步中
     */
    private String query;

    private boolean isUseDeFaultType=false;
}
