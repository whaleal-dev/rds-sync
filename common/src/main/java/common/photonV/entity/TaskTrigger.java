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
    private String taskName;
    private String procName;
    private String batchNo;
    private String batchType;
    private String isTurnover;
    private String brokerId;
    private String brokerIp;
    private Date createDt;
    private Date startDt;
    private Date completeDt;
    private Date lastupd;
    private String message;
    private String state;
    private String teamName;
    private String isNew;
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
