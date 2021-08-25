package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:日期字段类 年月日
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DateColumn extends AbstractColumn {
    private String data;

    public DateColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public String getData() {
        return this.data;
    }
}
