package dbconnection.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import common.photonV.entity.Datasource;
import dbconnection.mysql.MySqlConnection;
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
    public static void createMonoDbClient(String dsName, Datasource datasource) {
        if (mongoClientMap.containsKey(dsName)) {
            return;
        }
        MongoClient mongoClient = MongoClients.create(datasource.getUrl());
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
            mongoClientMap.remove(dsName);
        }
    }
}
