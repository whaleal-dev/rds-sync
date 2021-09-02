package com.whaleal.photon.source.oracle.parse;

import com.google.gson.Gson;
import common.column.*;
import common.dbtype.EnumOracleDataType;

import java.math.BigDecimal;
import java.sql.Blob;
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
    private static final Gson gson = new Gson();

    /**
     * us时间格式
     */
    private final static DateTimeFormatter formatterOfUs = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
    /**
     * 中国时间格式
     */
    private final static DateTimeFormatter formatterOfZh = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new StringColumn(columnName, "null");
        }
        //获取 mysql 值的数据类型
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumOracleDataType enumOracleDataType = EnumOracleDataType.valueOf(type);
        switch (enumOracleDataType) {
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case INT:
            case SMALLINT:
            case INTEGER:
            case NUMBER:
            case NUMBERIC:
                return new IntColumn(columnName, (Integer) object);
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case BINARY_DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case DECIMAL:
            case BIGDECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case DATE:
                return new DateColumn(columnName, (String) object);
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Timestamp) object).getTime());
            case BLOB:
                return new BlobColumn(columnName, (Blob) object);
            case CHAR:
            case NCHAR:
            case VARCHAR:
            case VARCHAR2:
            case STRING:
                return new StringColumn(columnName, object.toString());
            default:
                return new StringColumn(columnName, object.toString());
        }
    }

}
