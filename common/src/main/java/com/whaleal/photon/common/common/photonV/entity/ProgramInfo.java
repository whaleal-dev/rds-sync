package com.whaleal.photon.common.common.photonV.entity;

import com.whaleal.photon.common.cache.MemoryCache;
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
    private long batchNo = 0L;
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
     * 全量:all
     * 增量:inc
     * 实时:realTime
     * 全量批次表:allAndBatchNo
     * 查询批次表:queryAndBatchNo
     */
    private String syncMode;
    /**
     * 表过滤正则
     */
    private String dbTableWhite;
    /**
     * 是否同步DDL
     */
    private boolean isFilterDdl;
    /**
     * 已经存在的目标表是否删除
     */
    private boolean autoDropExistDbTable;
    /**
     * 是否创建索引
     */
    private boolean autoCreateIndex;
    /**
     * target任务线程数
     */
    private int targetThreadNum;
    /**
     * source任务线程数
     */
    private int sourceThreadNum;
    /**
     * 每个缓存区缓存批次数量
     */
    private int cacheBucketSize;
    /**
     * 缓存区个数
     */
    private int cacheBucketNum;
    /**
     * 每批次数据的大小
     */
    private int dataBatchSize;
    /**
     * 增量同步时，设置增量开始同步的时间。时间戳格式，单位s
     */
    private int startIncrementTime;
    /**
     * 在实时同步中每个数据源解析数据的线程
     */
    private int realTimeThreadNum;
    /**
     * 查询条件 / 自定义查询 SQL（仅查询模式）
     */
    private String query;
    /**
     * 自定义查询对应的库表名（仅查询模式）
     */
    private String dbTableName;
    /**
     * 目标端接收数据前执行的 SQL（仅查询模式）
     */
    private String preExecute;

    /** 兼容旧字段名：querySql → query */
    public String getQuerySql() {
        return query;
    }

    public void setQuerySql(String querySql) {
        this.query = querySql;
    }
    /**
     * 判断源和目标是否为同一类数据源
     */
    private boolean isUseDeFaultType = false;
    /**
     * 源端是否为rdb数据库
     */
    private boolean isRdbOfSource = false;
    /**
     * 全量同步时,是否按照顺序同步表或多表并行同步
     */
    private boolean parallelSynchronizationMultipleTables;
    /**
     * 延迟时间 单位秒
     */
    private int delayTime;
    /**
     * source 版本备注 单位秒
     */
    private String sourceVersion;
}
