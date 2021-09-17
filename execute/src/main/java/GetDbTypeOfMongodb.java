import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.core.datasource.DataSourceUtil;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import org.bson.*;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 5:06 下午
 */
public class GetDbTypeOfMongodb {
    static Gson gson = new Gson();

    public static void main(String[] args) {
//        Bson bson;
//        Document document = new Document();
//        document.append("BsonTimestamp1", new BsonTimestamp(1630655551, 78));
//        document.append("String", new String("str"));
//        document.append("Decimal128", new Decimal128(123));
//        document.append("BigDecimal", new BigDecimal(123));
//        document.append("Doc", new Document().append("1", 1));
//        document.append("javaint", 1);
//        document.append("bytes", new byte[]{1});
//        document.append("Array", new ArrayList<String>());
//        document.append("Binary data", new Binary(new byte[]{1, 2, 3}));
//        document.append("ObjectId", new ObjectId());
//        document.append("Boolean", new Boolean(false));
//        document.append("Date", new Date());
//        document.append("Null", null);
//        document.append("Regular Expression", new BsonRegularExpression("lhp.*"));
//        document.append("DBPointer", new BsonDbPointer("1", new ObjectId()));
//        document.append("Undefined", new BsonUndefined());
//        document.append("JavaScript", new BsonJavaScript("var i=0"));
//        document.append("Symbol", new BsonSymbol("var i=0"));
//        document.append("BsonStr", new BsonString("var i=0"));
//        document.append("BsonJavaScriptWithScope", new BsonJavaScriptWithScope("var i=0", new BsonDocument()));
//        document.append("32integer", new BsonInt32(12));
//        document.append("Timestamp", new Timestamp(System.currentTimeMillis()));
//        document.append("64int", new BsonInt64(123));
//        document.append("Min key", new BsonMinKey());
//        document.append("Max key", new BsonMaxKey());
//        document.append("BsonTimestamp", new BsonTimestamp());
//
//
//        Datasource mongodbDataSource = DataSourceUtil.getDataSourceByDsName("mongodb1");
//        MongoDbConnection.createMonoDbClient("mongodb1", mongodbDataSource);
//        MongoClient mongodbClient = MongoDbConnection.getMongoClient("mongodb1");
//        MongoCollection<Document> collection = mongodbClient.getDatabase("photon").getCollection("testDoc");
//        collection.drop();
//        collection.insertOne(document);
//        Document first = collection.find().first();
//
//
//        Iterator<Map.Entry<String, Object>> iterator = first.entrySet().iterator();
//
//        while (iterator.hasNext()) {
//            Map.Entry<String, Object> next = iterator.next();
//
//
//            if (next.getValue() != null) {
//                System.out.println(next.getKey() + "     value:" + next.getValue() + "         type:" + next.getValue().getClass().getSimpleName());
//            }
////            try {
////                // System.out.println(next.getValue().getClass());
////                String type = next.getValue().getClass().getSimpleName().toUpperCase();
////
////                System.out.print(type + "(\"" + type + "\"),");
////            } catch (Exception e) {
////
////            }
//
//        }
//
////
////        List list = new ArrayList();
////        list.add("1");
////        list.add(new Document().append("1", 1).append("id", new ObjectId()));
////        String s = gson.toJson(list);
////        List list1 = gson.fromJson(s.toString(), List.class);
////        System.out.println(s);
////        System.out.println(list1);
//
////        System.out.println("\n");
////        long l = System.currentTimeMillis();
////        BsonTimestamp bsonTimestamp = new BsonTimestamp(l);
////        System.out.println(bsonTimestamp);
////        System.out.println(bsonTimestamp.getTime());
////        System.out.println(bsonTimestamp.getInc());
////        System.out.println(bsonTimestamp.getValue());
////        System.out.println(l);
////
////        System.out.println("\n");
////        BsonTimestamp bsonTimestamp2 = new BsonTimestamp(1630655551, 78);
////        System.out.println(bsonTimestamp2.getValue());
////        System.out.println(bsonTimestamp2.getTime());
////        System.out.println(bsonTimestamp2.getInc());
//
//
//        BsonTimestamp bsonTimestamp3 = new BsonTimestamp(1631155899, 2);
//        System.out.println(bsonTimestamp3.getValue());
//        System.out.println(bsonTimestamp3.getTime());
//        System.out.println(bsonTimestamp3.getInc());


        Datasource mongodbDataSource = DataSourceUtil.getDataSourceByDsName("mongodb1");
        MongoDbConnection.createMonoDbClient("mongodb1", mongodbDataSource);
        MongoClient mongodbClient = MongoDbConnection.getMongoClient("mongodb1");
        // condition.append("_id", new Document("$lte", maxId).append("$gte", minId));
        BasicDBObject condition = new BasicDBObject();
//        condition.append("_id", new Document("$lte", new ObjectId("6139f53f6d7a24177185686c")).
//                append("$gte", new ObjectId("6139f53f6d7a24177185686c")));
        String a ="{$match:{\"_id\":{\"$oid\":\"6139f53f6d7a24177185686c\"}}}";
        Document parse =Document.parse(a);


        System.out.println(parse);
        System.out.println(gson.toJson(parse));
        ArrayList<Document> objects = new ArrayList<>();
        objects.add(parse);

        Document first = mongodbClient.getDatabase("photon").getCollection("test10000").aggregate(objects).first();
        Document dropDocument = new Document("drop", "test10000");
        System.out.println(gson.toJson(dropDocument));

        mongodbClient.getDatabase("photon").runCommand(dropDocument);

        System.out.println(first);


//
//
//        System.out.println(gson.toJson(new ObjectId()));
    }
}
