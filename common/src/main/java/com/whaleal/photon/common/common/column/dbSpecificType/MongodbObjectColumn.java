package com.whaleal.photon.common.common.column.dbSpecificType;

import com.whaleal.photon.common.common.column.AbstractColumn;

/**
 * @description:  MongodbObject字段类
 * @author: lhp
 * @time: 2021/8/24 11:05 上午
 */
public class MongodbObjectColumn extends AbstractColumn {
    private Object data;

    public MongodbObjectColumn(String columnName, Object object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }
}
