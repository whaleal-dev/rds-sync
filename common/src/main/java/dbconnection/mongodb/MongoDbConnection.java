package dbconnection.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mongodb链接类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class MongoDbConnection {
    /**
     * mongodb的链接
     */
    private static Map<String, MongoClient> mongoClientMap = new ConcurrentHashMap<>();

    /**
     * createMonoDbDataBase 创造mongodb客户端
     *
     * @param dsName
     * @desc 创造mongodb客户端。dcl检查
     */
    public static synchronized void createMonoDbClient(String dsName) {
        if (mongoClientMap.containsKey(dsName)) {
            return;
        }
        System.out.println(dsName);
        MongoClient mongoClient = MongoClients.create(dsName);
        mongoClientMap.put(dsName, mongoClient);
    }


    /**
     * getMongoClient 获取mongodb客户端
     *
     * @param dsName
     * @return MongoClient
     * @desc 获取mongodb客户端
     */
    public static MongoClient getMongoClient(String dsName) {
        if (!mongoClientMap.containsKey(dsName)) {
            createMonoDbClient(dsName);
        }
        return mongoClientMap.get(dsName);
    }

    /**
     * close 关闭mongodb客户端
     *
     * @param dsName
     * @desc 关闭mongodb客户端
     */
    public static void close(String dsName) {
        if (mongoClientMap.containsKey(dsName)) {
            mongoClientMap.get(dsName).close();
        }
    }

    public static void main(String[] args) {
        Document document = new Document();
        System.out.println(document.get("_id").toString());
    }

}
