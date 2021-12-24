package com.whaleal.photon.core.trigger;

import com.whaleal.photon.common.common.photonV.entity.TaskTrigger;
import com.whaleal.photon.core.dbconnection.MetadataConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.common.util.Log;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/30 1:50 下午
 */
public class TriggerUtil {
    private static final JdbcTemplate JDBC_TEMPLATE = MetadataConnection.getJdbcTemplate();

    public static void insertTriggerInfo(TaskTrigger taskTrigger) {
        String sql = "insert into photon.task_trigger(id,task_name,proc_name,batch_no,state)";
        String values = "values(?,?,?,?,?)";
        JDBC_TEMPLATE.update(sql + values, taskTrigger.getId(), taskTrigger.getTaskName(), taskTrigger.getProName(), taskTrigger.getBatchNo(), "new");
    }

    public static void updateTriggerInfo(TaskTrigger taskTrigger) {
        try {
            String sql = "update  photon.task_trigger set state=? where id=? and state!='stop' ";
            JDBC_TEMPLATE.update(sql, taskTrigger.getState(), taskTrigger.getId());
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    public static String getTriggerTateInfo(String id) {
        String state = "";
        try {
            String sql = "select `state`  from photon.task_trigger where id='" + id + "'";
            // System.out.println(sql);
            state = JDBC_TEMPLATE.queryForObject(sql, String.class);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        return state;
    }

    public static void main(String[] args) {
        String triggerTateInfo = getTriggerTateInfo("468859063fa8448398afbe99423e7494");
        System.out.println(triggerTateInfo);
    }
}
