package com.whaleal.rds.common.common.column;

import java.sql.Blob;

/**
 * Blob字段类
 *
 * @author: jy
 * @Date: 2021/08/26
 * @desc: 可以使用byte来替代
 */
public class BlobColumn extends AbstractColumn {

    private Blob data;

    public BlobColumn(String columnName, Blob object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Blob getData() {
        return this.data;
    }

}
