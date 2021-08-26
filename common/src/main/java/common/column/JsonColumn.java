package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @description:json字段类 json字符串
 * @author: lhp
 * @time: 2021/8/23 9:55 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JsonColumn extends AbstractColumn {
    private String data;

    public JsonColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }
}
