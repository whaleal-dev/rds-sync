package parse;


import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import common.column.*;
import common.dbtype.EnumMongoDbDataType;
import dbconnection.mongodb.MongoDbConnection;
import org.bson.*;
import org.bson.types.Code;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * @author liheping
 */
public class TransformationMongodbDataToColumn {

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
            return new StringColumn(columnName, "null");
        }
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumMongoDbDataType enumMongoDbDataType = EnumMongoDbDataType.valueOf(type);
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
                System.out.println("3：   " + object.toString());
                TemporalAccessor temporalAccessor = formatterOfUs.parse(object.toString());
                System.out.println("2：   " + temporalAccessor.toString());
                String formatterDate = formatterOfZh.format(temporalAccessor);
                System.out.println("1：   " + formatterDate);
                return new DateTimeColumn(columnName, (((Date)object).getTime()));
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
                return new ArrayColumn(columnName, (List<Object>) object);
            case DOCUMENT:
                return new JsonColumn(columnName, gson.toJson(object));
            case OBJECTID:
                return new ObjectIdColumn(columnName, (ObjectId) object);
            case STRING:
            default:
                return new StringColumn(columnName, object.toString());
        }


    }

    public static void main(String[] args) {
        BasicDBObject basicDBObject = new BasicDBObject();
        basicDBObject.append("_id", new ObjectId("60efd2a0c5a4e52f3f978d3d"));
        MongoClient mongoClient = MongoDbConnection.getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        Document first = mongoClient.getDatabase("photon").getCollection("apply").find(basicDBObject).first();
        System.out.println(first.get("time"));
        TemporalAccessor temporalAccessor = formatterOfUs.parse(first.get("time").toString());
        System.out.println("2：   " + temporalAccessor.toString());
        String formatterDate = formatterOfZh.format(temporalAccessor);
        System.out.println(formatterDate);
       // System.out.println(first.get("time",BsonDateTime.class));
        //System.out.println(first.get("time").getClass().getSimpleName());
        System.out.println(first.getDate("time").getTime());
    }
}
