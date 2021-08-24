package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StringColumn extends AbstractColumn {
    private String data;
    public StringColumn(String columnName, String object) {
        this.columnName=columnName;
        this.data=object;
    }
    @Override
    public String getData() {
        return this.data;
    }
}
