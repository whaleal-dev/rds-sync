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
@ToString
@NoArgsConstructor
public abstract class AbstractColumn {
    protected String columnName;

    public abstract Object getData();
}
