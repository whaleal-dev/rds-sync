package com.whaleal.photon.common.common.operation;


/**
 * @author lhp
 * @time 2021-05-31 13:12:12
 * @desc 数据类操作行为
 */
public final class OperationFlag {
    /**
     * 清空表
     */
    public static final String TRUNCATE = "TRUNCATE";
    /**
     * 更新
     */
    public static final String UPDATE = "UPDATE";
    /**
     * 删除
     */
    public static final String DELETE = "DELETE";
    /**
     * 删除表
     */
    public static final String DROPTABLE = "DROPTABLE";
    /**
     * 插入
     */
    public static final String INSERT = "INSERT";
    /**
     * 批量插入
     */
    public static final String INSERTMANY = "INSERTMANY";
    /**
     * CDC / 日志类增量操作标记（非 Mongo oplog）。
     */
    public static final String CDC = "CDC";
    /**
     * @deprecated 使用 {@link #CDC}
     */
    @Deprecated
    public static final String OPLOG = CDC;
    /**
     * 添加字段
     */
    public static final String ADDCOLUMN = "ADDCOLUMN";
    /**
     * 删除字段
     */
    public static final String DROPCOLUMN = "DROPCOLUMN";
    /**
     * 表重命名
     */
    public static final String RENAMETABLE = "RENAMETABLE";
    /**
     * 创建索引
     */
    public static final String CREATEINDEX = "CREATEINDEX";
    /**
     * 删除索引
     */
    public static final String DROPINDEX = "DROPINDEX";
    /**
     * 删除库
     */
    public static final String DROPDATABASE = "DROPDATABASE";
}
