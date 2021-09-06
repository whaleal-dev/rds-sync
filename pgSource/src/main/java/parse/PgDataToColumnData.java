package parse;

import common.column.*;
import common.dbtype.java.EnumPgDataInJavaType;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;


/**
 * @author liheping
 */
public class PgDataToColumnData {

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new NullColumn(columnName, null);
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        if (type.startsWith("PG")) {
            type = "PGOBJECT";
        } else if (("BYTE[]").equals(type)) {
            type = "BYTES";
        }
        EnumPgDataInJavaType dataType = EnumPgDataInJavaType.valueOf(type);
        switch (dataType) {
            case PGOBJECT:
                return new PgObjectColumn(columnName, (PGobject) object);
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case DATE:
                return new DateColumn(columnName, object.toString());
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case BIGDECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case TIME:
                return new TimeColumn(columnName, object.toString());
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Timestamp) object).getTime());
            case BOOLEAN:
                return new BoolColumn(columnName, ((Boolean) object));
            case BYTES:
                return new BytesColumn(columnName, ((byte[]) object));
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }
    }

}
