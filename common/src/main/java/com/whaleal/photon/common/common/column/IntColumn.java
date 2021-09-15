package com.whaleal.photon.common.common.column;

/**
 * @description:int字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */

public class IntColumn extends AbstractColumn {
    private int data;

    public IntColumn(String columnName, Integer object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Integer getData() {
        return this.data;
    }
}
