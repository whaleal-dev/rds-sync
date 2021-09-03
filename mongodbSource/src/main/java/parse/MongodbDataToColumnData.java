package parse;


import com.google.gson.Gson;
import common.column.*;
import common.dbtype.java.EnumMongoDbDataInJavaType;
import org.bson.*;
import org.bson.types.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * @author liheping
 */
public class MongodbDataToColumnData {
    private static final Gson gson = new Gson();
    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new NullColumn(columnName, null);
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumMongoDbDataInJavaType enumMongoDbDataInJavaType = EnumMongoDbDataInJavaType.valueOf(type);
        switch (enumMongoDbDataInJavaType) {
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case LONG:
                return new LongColumn(columnName, (Long) object);
            case BINARY:
                return new BytesColumn(columnName, ((Binary) object).getData());
            case DECIMAL128:
                return new BigDecimalColumn(columnName, new BigDecimal(((Decimal128) object).doubleValue()));
            case DATE:
                return new DateTimeColumn(columnName, (((Date) object).getTime()));
            case BSONREGULAREXPRESSION:
                BsonRegularExpression bsonRegularExpression = (BsonRegularExpression) object;
                String options = bsonRegularExpression.getOptions();
                String pattern = bsonRegularExpression.getPattern();
                String value = "options:" + options + ",pattern:" + pattern;
                return new StringColumn(columnName, value);
            case CODE:
                Code code = (Code) object;
                return new StringColumn(columnName, code.getCode());
            case BSONTIMESTAMP:
                return new TimestampColumn(columnName, ((BsonTimestamp) object).getValue());
            case SYMBOL:
                Symbol symbol = (Symbol) object;
                return new StringColumn(columnName, symbol.getSymbol());
            case BOOLEAN:
                return new BoolColumn(columnName, ((Boolean) object).booleanValue());
            case ARRAYLIST:
                return new ArrayColumn(columnName, (List<Object>) object);
            case DOCUMENT:
                return new JsonColumn(columnName, gson.toJson(object));
            case OBJECTID:
                return new ObjectIdColumn(columnName, (ObjectId) object);
            case STRING:
            case BSONDBPOINTER:
            case BSONUNDEFINED:
            case CODEWITHSCOPE:
            case MAXKEY:
            case MINKEY:
            default:
                return new StringColumn(columnName, object.toString());
        }
    }

//    public static void main(String[] args) {
//        BsonMinKey key= new BsonMinKey("1");
//        key.toString();
//    }
}
