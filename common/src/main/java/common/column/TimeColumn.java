package common.column;

/**
 * @description: 时分秒数据类型
 * @author: lhp
 * @time: 2021/9/1 2:35 下午
 */
public class TimeColumn extends AbstractColumn {
    private long data;

    public TimeColumn(String columnName, Long object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Long getData() {
        return this.data;
    }
}
