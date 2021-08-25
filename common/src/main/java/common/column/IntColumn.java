package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:int字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class IntColumn extends AbstractColumn {
    private int data;

    public IntColumn(String columnName, Integer object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }
}
