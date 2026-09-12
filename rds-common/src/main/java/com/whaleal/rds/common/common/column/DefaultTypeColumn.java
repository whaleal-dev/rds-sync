package com.whaleal.rds.common.common.column;


/**
 * @desc: 默认字段类 检测到源端和 Sink 端是同种数据类型 可以不进行数据转换
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class DefaultTypeColumn extends AbstractColumn {
    private Object data;

    public DefaultTypeColumn(String columnName, Object object) {
        this.data = object;
        this.columnName = columnName;
    }

    @Override
    public Object getData() {
        return data;
    }
}
