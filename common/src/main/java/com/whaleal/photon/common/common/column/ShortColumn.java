package com.whaleal.photon.common.common.column;



/**
 * @description:short字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
public class ShortColumn extends AbstractColumn {
    private short data;

    public ShortColumn(String columnName, short object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Short getData() {
        return this.data;
    }
}
