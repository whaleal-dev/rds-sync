package com.whaleal.photon.source.mysql.parse;

import com.whaleal.photon.common.common.column.*;
import com.whaleal.photon.common.common.columntype.java.EnumMySqlDataInJavaType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 转换mysql数据的列
 *
 * @author: jy
 * @Date: 2021/08/26
 */
public class MysqlDataToColumnData {

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new NullColumn(columnName, null);
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        //switch不能识别byte[]
        if (("BYTE[]").equals(type)) {
            type = "BYTES";
        }
        EnumMySqlDataInJavaType enumMySqlDataInJavaType = EnumMySqlDataInJavaType.valueOf(type);
        switch (enumMySqlDataInJavaType) {
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case BIGDECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case DATE:
                return new DateColumn(columnName, object.toString());
            case TIME:
                return new TimeColumn(columnName, object.toString());
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Date) object).getTime());
            case BYTES:
                return new BytesColumn(columnName, ((byte[]) object));
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }
    }
}
