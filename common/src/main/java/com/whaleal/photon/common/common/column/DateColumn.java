package com.whaleal.photon.common.common.column;

/**
 * @description:日期字段类 年月日 时间格式 2021-08-23
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */

public class DateColumn extends AbstractColumn {
    private String data;

    public DateColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public String getData() {
        return this.data;
    }
}
