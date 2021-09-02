package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description: long字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LongColumn extends AbstractColumn {
    private long data;

    public LongColumn(String columnName, long object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Long getData() {
        return this.data;
    }
}
