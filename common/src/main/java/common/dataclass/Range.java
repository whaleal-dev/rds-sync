package common.dataclass;


import lombok.*;

/**
 * @author: lhp
 * @time: 2021/7/19 5:02 下午
 * @desc:数据分片范围
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Range {
    /**
     * 最大值
     */
    private Object maxId;
    /**
     * 最小值
     */
    private Object minId;
    /**
     * 是否为边缘值
     */
    private boolean isMax = false;
    /**
     * 库表名
     */
    private String dbTableName;
    /**
     * 开始时间
     */
    private long startTime;
    /**
     * 结束时间
     */
    private long endTime;
    /**
     * 数据类型
     */
    private int _idType;
    /**
     * 查询语句
     */
    private String query;
}
