package com.whaleal.rds.common.common.column;


/**
 * @description:json字段类 json字符串
 * @author: lhp
 * @time: 2021/8/23 9:55 上午
 */

public class JsonColumn extends AbstractColumn {
    private String data;

    public JsonColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public String getData() {
        return this.data;
    }
}
