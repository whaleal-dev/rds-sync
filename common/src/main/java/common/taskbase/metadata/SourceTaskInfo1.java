package common.taskbase.metadata;

import lombok.*;

/**
 * 任务配置信息
 *
 * @author: jy
 * @Date: 2021/08/25
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
public class SourceTaskInfo1 {
    /**
     * RangeSql
     */
    private String rangeSql;
    /**
     * 源数据url
     */
    private String sourceUrl;
    /**
     * 源数据用户名
     */
    private String sourceUsername;
    /**
     * 源数据密码
     */
    private String sourcePassword;
    /**
     * 源数据库名
     */
    private String sourceDatabase;
    /**
     * 源数据表名
     */
    private String soureTable;
    /**
     * 目标数据url
     */
    private String targetUrl;
    /**
     * 目标数据用户名
     */
    private String targetUsername;
    /**
     * 目标数据密码
     */
    private String targetPassword;
    /**
     * 目标数据库名
     */
    private String targetDatabase;
    /**
     * 目标数据集合名
     */
    private String targetCollection;

    private Integer dataBatchSize;
//
//    public SourceTaskInfo(Range range, String dbTableName, String sourceDsName) {
//        this.range = range;
//        this.dbTableName = dbTableName;
//        this.sourceDsName = sourceDsName;
//    }
}
