package common.column;

import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 2:37 下午
 */
public class BytesColumn extends AbstractColumn {
    private Byte[] data;

    public BytesColumn(String columnName, Byte[] object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Byte[] getData() {
        return this.data;
    }


}
