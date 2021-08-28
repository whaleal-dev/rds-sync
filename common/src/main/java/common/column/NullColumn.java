package common.column;

import java.util.List;

/**
 * @desc: 数组字段类
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class NullColumn extends AbstractColumn {
    private Object data;

    public NullColumn(String columnName, Object object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }

}
