package com.whaleal.rds.common.common.column;

/**
 * @description: float字段类
 * @author: lhp
 * @time: 2021/8/23 9:54 上午
 */

public class FloatColumn extends AbstractColumn {
    private float data;

    public FloatColumn(String columnName, float object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Float getData() {
        return this.data;
    }
}
