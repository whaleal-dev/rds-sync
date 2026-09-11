package com.whaleal.photon.common.common.dataclass;

import lombok.*;

/**
 * 数据分片范围（关系型全量切分）。
 * <ul>
 *   <li>{@link #queryCondition}：WHERE 条件片段</li>
 *   <li>{@link #query}：完整 SQL（任务执行时读取）</li>
 * </ul>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Range {
    private String columnName;
    /** 最大值 */
    private Object maxValue;
    /** 最小值 */
    private Object minValue;
    /** 是否为边缘值（闭区间上界） */
    private boolean isMax = false;
    /** 库表名 */
    private String dbTableName;
    private long startTime;
    private long endTime;
    /** 数据类型 */
    private int type;
    /** 查询条件（一般为 SQL where 片段） */
    private Object queryCondition;
    /** 完整查询（一般为 SQL） */
    private Object query;
    /** 查询范围条数 */
    private int rangeSize;

    /** 兼容旧命名 maxId */
    public Object getMaxId() {
        return maxValue;
    }

    public void setMaxId(Object maxId) {
        this.maxValue = maxId;
    }

    /** 兼容旧命名 minId */
    public Object getMinId() {
        return minValue;
    }

    public void setMinId(Object minId) {
        this.minValue = minId;
    }

    /** 兼容旧命名 sql（完整 SQL） */
    public Object getSql() {
        return query;
    }

    public void setSql(Object sql) {
        this.query = sql;
    }
}
