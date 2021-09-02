package trigger;

import common.photonV.entity.TaskTrigger;
import dbconnection.MetadataConnection;
import dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import util.StringUtil;

import java.util.Map;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/30 1:50 下午
 */
public class TriggerUtil {
    private static JdbcTemplate jdbcTemplate = MetadataConnection.getJdbcTemplate();

    public static void insertTriggerInfo(TaskTrigger taskTrigger) {
        String sql = "insert into photon.task_trigger(id,task_name,proc_name,state)";
        String values = "values(?,?,?,?)";
        jdbcTemplate.update(sql + values, taskTrigger.getId(), taskTrigger.getTaskName(), taskTrigger.getProcName(), "new");
    }

    public static void updateTriggerInfo(TaskTrigger taskTrigger) {
        String sql = "update  photon.task_trigger set state=? where id=?";
        jdbcTemplate.update(sql, taskTrigger.getState(), taskTrigger.getId());
    }

    public static String getTriggerTateInfo(String id) {
        String sql = "select state  photon.task_trigger where id='" + id + "'";
        return jdbcTemplate.queryForObject(sql, String.class);
    }
}
