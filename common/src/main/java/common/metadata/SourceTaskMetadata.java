package common.metadata;

import common.dataclass.Range;
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
public class SourceTaskMetadata {
    /**
     * range
     */
    private Range range;
    /**
     * mongoNamespace
     */
    private String dbTableName;
    /**
     * 源数据源名称
     */
    private String sourceDsName;
    /**
     * 目标数据源名称
     */
    private String targetDsName;

    public SourceTaskMetadata(Range range, String dbTableName, String sourceDsName) {
        this.range = range;
        this.dbTableName = dbTableName;
        this.sourceDsName = sourceDsName;
    }
}
