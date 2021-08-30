package trigger;

import common.photonV.entity.TaskTrigger;
import dbconnection.mysql.MySqlConnection;
import util.StringUtil;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/30 1:50 下午
 */
public class TriggerUtil {

    public static void insertTriggerInfo(TaskTrigger taskTrigger) {
        String sql = "insert into photon.task_trigger(id,task_name,proc_name,state)";
        String values = "values(?,?,?,?)";
        MySqlConnection.getJdbcTemplate("1").update(sql + values, taskTrigger.getId(), taskTrigger.getTaskName(), taskTrigger.getProcName(), "new");
    }

    public static void updateTriggerInfo(TaskTrigger taskTrigger) {
        String sql = "update  photon.task_trigger set state=? where id=?";
        MySqlConnection.getJdbcTemplate("1").update(sql, taskTrigger.getState(), taskTrigger.getId());
    }

    public static String getTriggerTateInfo(String id) {
        String sql = "select state  photon.task_trigger where id='" + id + "'";
        return MySqlConnection.getJdbcTemplate("1").queryForObject(sql, String.class);
    }
}
