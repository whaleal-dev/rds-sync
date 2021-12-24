package com.whaleal.photon.source.mongodb.parse;


import com.google.gson.Gson;
import com.whaleal.photon.common.common.column.*;
import com.whaleal.photon.common.common.column.dbSpecificType.MongodbObjectColumn;
import com.whaleal.photon.common.common.columntype.java.EnumMongoDbDataInJavaType;
import org.bson.*;
import org.bson.types.*;
import java.math.BigDecimal;
import java.util.Date;


/**
 * @author liheping
 */
public class MongodbDataToColumnData {
    private static final Gson GSON = new Gson();

    public static AbstractColumn parseValue(String columnName, Object object, boolean isDefaultType) {
        if (isDefaultType) {
            return new DefaultTypeColumn(columnName, object);
        } else if (object == null) {
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
                return new BigDecimalColumn(columnName, BigDecimal.valueOf(((Decimal128) object).doubleValue()));
            case DATE:
                return new DateTimeColumn(columnName, (((Date) object).getTime()));
            case BSONTIMESTAMP:
                BsonTimestamp bsonTimestamp = (BsonTimestamp) object;
                return new TimestampColumn(columnName, bsonTimestamp.getTime(), bsonTimestamp.getInc());
            case BOOLEAN:
                return new BoolColumn(columnName, (Boolean) object);
            case ARRAYLIST:
                return new ArrayColumn(columnName, object);
            case DOCUMENT:
                return new JsonColumn(columnName, GSON.toJson(object));
            case STRING:
                return new StringColumn(columnName, object.toString());
            case OBJECTID:
            case BSONDBPOINTER:
            case BSONUNDEFINED:
            case CODEWITHSCOPE:
            case MAXKEY:
            case MINKEY:
            case SYMBOL:
            case CODE:
            case BSONREGULAREXPRESSION:
            default:
                return new MongodbObjectColumn(columnName, object);
        }
    }
}
