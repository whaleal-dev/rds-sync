package com.whaleal.photon.common.common.dataclass;

import com.whaleal.photon.common.common.column.AbstractColumn;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lhp
 * @time 2021-05-31 13:12:12
 * @desc 批量数据实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BatchDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 批次号
     */
    private long batchNo;
    /**
     * 操作行为
     */
    private String operation;
    /**
     * 库表名
     */
    private String dbTableName;
    /**
     * 数据来源
     */
    private String sourceDsName;
    /**
     * 数据目的地
     */
    private String targetDsName;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 程序名称
     */
    private String proName;
    /**
     * 数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();
}
