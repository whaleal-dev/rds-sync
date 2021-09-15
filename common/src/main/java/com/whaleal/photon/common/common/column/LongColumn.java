package com.whaleal.photon.common.common.column;

/**
 * @description: long字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */

public class LongColumn extends AbstractColumn {
    private long data;

    public LongColumn(String columnName, long object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Long getData() {
        return this.data;
    }
}
