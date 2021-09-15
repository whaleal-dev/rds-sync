package com.whaleal.photon.common.common.column;

/**
 * @description: 时分秒数据类型
 * @author: lhp
 * @time: 2021/9/1 2:35 下午
 */
public class TimeColumn extends AbstractColumn {
    private String data;

    public TimeColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public String getData() {
        return this.data;
    }
}
