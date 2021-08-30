package common.taskbase;

import common.dataclass.Range;
import lombok.*;

/**
 * 任务配置信息
 *
 * @author: jy
 * @Date: 2021/08/25
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class MysqlSourceTaskInfo {
    /**
     * RangeSql
     */
    private String rangeSql;
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

    public MysqlSourceTaskInfo(String rangeSql, String dbTableName, String sourceDsName) {
        this.rangeSql = rangeSql;
        this.dbTableName = dbTableName;
        this.sourceDsName = sourceDsName;
    }
}
