package com.whaleal.photon.source.oracle.parse;

import common.column.*;
import common.dbtype.java.EnumOracleDataInJavaType;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 转换oracle数据列
 *
 * @author cs
 * @date 2021/08/31
 */
public class OracleDataToColumnData {

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new StringColumn(columnName, "null");
        }
        //获取 mysql 值的数据类型
        String type = object.getClass().getSimpleName().toUpperCase();
        if (("BYTE[]").equals(type)) {
            type = "BYTES";
        }
        EnumOracleDataInJavaType enumOracleDataInJavaType = EnumOracleDataInJavaType.valueOf(type);
        switch (enumOracleDataInJavaType) {
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
