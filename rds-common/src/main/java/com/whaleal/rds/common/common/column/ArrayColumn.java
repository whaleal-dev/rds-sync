package com.whaleal.rds.common.common.column;

import com.google.gson.Gson;
import com.whaleal.rds.common.util.Log;


/**
 * @desc: 数组字段类
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class ArrayColumn extends AbstractColumn {
    private static final Gson GSON = new Gson();

    private final Object data;

    public ArrayColumn(String columnName, Object object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringBuilder.append(GSON.toJson(this.data));
        } catch (Exception e) {
            Log.error("解析ArrayColumn出现异常,错误信息:"+e.getMessage());
            stringBuilder = new StringBuilder();
        }
        return stringBuilder.toString();
    }
}
