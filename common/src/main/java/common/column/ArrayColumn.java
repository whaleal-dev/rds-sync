package common.column;

import java.util.List;

/**
 * @desc: 数组字段类
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class ArrayColumn extends AbstractColumn {
    private List<Object> data;

    public ArrayColumn(String columnName, List<Object> object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public List<Object> getData() {
        return this.data;
    }

    @Override
    public String toString() {
        // 防止出现[@的数据
        StringBuilder stringBuilder = new StringBuilder();
        for (Object object : data) {
            stringBuilder.append(object.toString());
        }
        return stringBuilder.toString();
    }
}
