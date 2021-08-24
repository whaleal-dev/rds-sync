package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 2:15 下午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BoolColumn extends AbstractColumn {
    private boolean data;

    public BoolColumn(String columnName, Boolean object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Boolean getData() {
        return this.data;
    }
}