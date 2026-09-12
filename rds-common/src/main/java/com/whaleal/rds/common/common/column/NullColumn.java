package com.whaleal.rds.common.common.column;


/**
 * @desc: null字段类
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class NullColumn extends AbstractColumn {

    public NullColumn(String columnName, Object object) {
        this.columnName = columnName;
    }

    @Override
    public Object getData() {
        return null;
    }
}
