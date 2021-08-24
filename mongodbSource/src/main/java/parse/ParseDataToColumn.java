package parse;


import com.google.gson.Gson;
import common.column.*;
import common.dbtype.EnumMongoDbDataType;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.types.Code;
import org.bson.types.Decimal128;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;


public class ParseDataToColumn {


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
            return null;
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumMongoDbDataType enumMongoDbDataType = EnumMongoDbDataType.valueOf(type);
        BsonTimestamp bsonTimestamp=new BsonTimestamp();

        switch (enumMongoDbDataType) {
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case DECIMAL128:
                return new DoubleColumn(columnName, ((Decimal128) object).doubleValue());
            case DATE:
                TemporalAccessor temporalAccessor = formatterOfUs.parse(object.toString());
                String formatterDate = formatterOfZh.format(temporalAccessor);
                return new DateColumn(columnName, formatterDate);
            case REGULAR:
                BsonRegularExpression bsonRegularExpression = (BsonRegularExpression) object;
                String options = bsonRegularExpression.getOptions();
                String pattern = bsonRegularExpression.getPattern();
                String value = "options:" + options + ",pattern:" + pattern;
                return new StringColumn(columnName, value);
            case CODE:
                Code code = (Code) object;
                return new StringColumn(columnName, code.getCode());
            case BOOLEAN:
                return new BoolColumn(columnName, ((Boolean) object).booleanValue());
            case ARRAYLIST:
            case DOCUMENT:
                return new JsonColumn(columnName, gson.toJson(object));
            case OBJECTID:
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }

    }

}
