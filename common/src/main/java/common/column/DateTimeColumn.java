package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:日期时间字段类 年月日时分秒 或毫秒
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */

public class DateTimeColumn extends AbstractColumn {
    private Long data;

    public DateTimeColumn(String columnName, Long object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Long getData() {
        return this.data;
    }
}
