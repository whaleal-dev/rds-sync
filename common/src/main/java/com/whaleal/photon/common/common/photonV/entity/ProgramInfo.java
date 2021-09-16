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
    private boolean isDropExistDbTable;
    /**
     * 是否创建索引
     */
    private boolean isCreateIndex;
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
    private int cacheSize;
    /**
     * 缓存区个数
     */
    private int cacheNum;
    /**
     * 每个批次数据的大小
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
    /**
     * 判断源和目标是否为同一类数据源
     */
    private boolean isUseDeFaultType = false;
    /**
     * 源端是否为rdb数据库
     */
    private boolean isRdbOfSource = false;
}
