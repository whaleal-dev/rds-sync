package common.column;

/**
 * @description:byte数组 适用于二进制对象
 * @author: lhp
 * @time: 2021/9/1 2:37 下午
 */
public class BytesColumn extends AbstractColumn {
    private byte[] data;

    public BytesColumn(String columnName, byte[] object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public byte[] getData() {
        return this.data;
    }
}
