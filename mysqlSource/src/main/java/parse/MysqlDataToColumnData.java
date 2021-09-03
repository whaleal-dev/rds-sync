package parse;

import common.column.*;
import common.dbtype.java.EnumMySqlDataInJavaType;

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
        System.out.println("type      "+type);
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
                return new DateTimeColumn(columnName, ((Date) object).getTime());
            case TIME:
                return new TimeColumn(columnName, ((Date) object).getTime());
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
