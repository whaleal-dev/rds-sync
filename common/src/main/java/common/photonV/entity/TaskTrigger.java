package common.photonV.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author liheping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTrigger {
    /**
     * 任务名
     */
    private String taskName;
    /**
     * 程序名
     */
    private String procName;
    /**
     * 批次号
     */
    private long batchNo;
    /**
     * 批次类型
     */
    private String batchType;

    private String isTurnover;
    /**
     * 执行主机id
     */
    private String brokerId;
    /**
     * 执行主机ip
     */
    private String brokerIp;

    private Date createDt;
    /**
     * 任务开始执行时间
     */
    private Date startDt;
    /**
     * 任务完成时间
     */
    private Date completeDt;

    private Date lastUpd;
    /**
     * 执行信息
     */
    private String message;
    /**
     * 执行状态 new start running stop
     */
    private String state;
    /**
     * 团队名
     */
    private String teamName;
    private String isNew;
    /**
     * 优先级
     */
    private Integer priority;
    private Integer duration;
    private String workgroup;
    private String memberName;
    private Integer runState;
    private Integer isValid;
    private String rtnCode;
    private Integer actualRedoNum;
    private Long avgDuration;
    private String id;
    private String parentCode;
    private String initiatedTeam;
}
