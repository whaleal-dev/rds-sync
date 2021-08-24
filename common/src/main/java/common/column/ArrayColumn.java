package common.column;

import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class ArrayColumn extends AbstractColumn {
    private List<Object> data;

    public ArrayColumn(String columnName, List<Object>  object) {
        this.columnName=columnName;
        this.data=object;
    }

    @Override
    public Object getData() {
        return this.data;
    }

}
