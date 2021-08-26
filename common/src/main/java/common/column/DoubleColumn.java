package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:double字段类
 * @author: lhp
 * @time: 2021/8/23 9:54 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DoubleColumn extends AbstractColumn {
    private double data;

    public DoubleColumn(String columnName, double object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Double getData() {
        return this.data;
    }

}
