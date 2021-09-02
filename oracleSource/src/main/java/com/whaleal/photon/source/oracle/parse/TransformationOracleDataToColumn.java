package com.whaleal.photon.source.oracle.parse;

import com.google.gson.Gson;
import common.column.*;
import common.dbtype.EnumOracleDataType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 转换oracle数据列
 *
 * @author cs
 * @date 2021/08/31
 */
public class TransformationOracleDataToColumn {

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new StringColumn(columnName, "null");
        }
        //获取 mysql 值的数据类型
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumOracleDataType enumOracleDataType = EnumOracleDataType.valueOf(type);
        switch (enumOracleDataType) {
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case BIGDECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Timestamp) object).getTime());
            case BYTES:
                return new BytesColumn(columnName, (byte[]) object);
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case BFILE:
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }
    }

}
