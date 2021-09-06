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

public class TimestampColumn extends AbstractColumn {
    private long data;

    public TimestampColumn(String columnName, long object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Long getData() {
        return this.data;
    }

}