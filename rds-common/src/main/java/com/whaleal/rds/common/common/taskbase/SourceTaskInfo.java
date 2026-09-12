package com.whaleal.rds.common.common.taskbase;

import com.whaleal.rds.common.common.dataclass.Range;
import lombok.*;

/**
 * @desc: 任务配置信息
 * @author: lhp
 * @time: 2021/7/19 5:36 下午
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class SourceTaskInfo {
    /**
     * range
     */
    private Range range;
    /**
     * dbTableName
     */
    private String dbTableName;
    /**
     * 源数据源名称
     */
    private String sourceDsName;
    /**
     * Sink 数据源名称
     */
    private String sinkDsName;
    /**
     * 开始时间
     */
    private long startTime;
    /**
     * 结束时间
     */
    private long endTime;

    public SourceTaskInfo(Range range, String dbTableName, String sourceDsName) {
        this.range = range;
        this.dbTableName = dbTableName;
        this.sourceDsName = sourceDsName;
    }

}
