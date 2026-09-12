package com.whaleal.rds.common.common.column;

/**
 * @description: 字符串字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */

public class StringColumn extends AbstractColumn {
    private String data;

    public StringColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public String getData() {
        return this.data;
    }
}
