package com.whaleal.rds.common.common.column;


/**
 * @description:布尔字段类
 * @author: lhp
 * @time: 2021/8/23 2:15 下午
 */
public class BoolColumn extends AbstractColumn {
    private boolean data;

    public BoolColumn(String columnName, Boolean object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Boolean getData() {
        return this.data;
    }
}
