package parse;


import com.google.gson.Gson;
import common.column.*;

import common.dbtype.EnumPgDataInJavaType;
import org.postgresql.util.PGobject;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


/**
 * @author liheping
 */
public class TransformationPgDataToColumnData {

    private static final Gson gson = new Gson();
    /**
     * us时间格式
     */
    private final static DateTimeFormatter formatterOfUs = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
    /**
     * 中国时间格式
     */
    private final static DateTimeFormatter formatterOfZh = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSz", Locale.CHINA);

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new NullColumn(columnName, null);
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumPgDataInJavaType dataType = EnumPgDataInJavaType.valueOf(type);
        switch (dataType) {
            case PGOBJECT:
                return new PgObjectColumn(columnName, (PGobject) object);
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case BIGDECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case TIME:
                return new TimeColumn(columnName, ((Time) object).getTime());
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Timestamp) object).getTime());
            case BOOLEAN:
                return new BoolColumn(columnName, ((Boolean) object));
            case BYTEARRAY:
                return new BytesColumn(columnName, ((byte[]) object));
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }


    }

//    public static void main(String[] args) {
//
//        Time time = new Time();
//
//    }
}
