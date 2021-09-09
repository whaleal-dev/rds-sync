package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description: 时间戳字段类
 * @author: lhp
 * @time: 2021/8/23 2:15 下午
 */
@Data
public class TimestampColumn extends AbstractColumn {
    private long data;
    private int seconds;
    private int inc = 0;

    public TimestampColumn(String columnName, long object) {
        this.columnName = columnName;
        this.data = object;
    }


    public TimestampColumn(String columnName, int seconds, int inc) {
        this.columnName = columnName;
        this.seconds = seconds;
        this.inc = inc;
    }


    @Override
    public Long getData() {
        System.out.println((seconds * 1000L) + inc);
        if (seconds > 0) {
            return ((long) ((seconds * 1000L) + inc));
        }
        return this.data;
    }

}