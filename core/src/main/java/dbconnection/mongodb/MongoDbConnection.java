package dbconnection.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import common.photonV.entity.Datasource;
import dbconnection.mysql.MySqlConnection;
import org.bson.Document;
import util.Log;

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
     * @param procNameAndBatchNoAndDsName
     * @desc 创造mongodb客户端。dcl检查
     */
    public static void createMonoDbClient(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (mongoClientMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        MongoClient mongoClient = MongoClients.create(datasource.getUrl());
        mongoClientMap.put(procNameAndBatchNoAndDsName, mongoClient);
    }

    /**
     * getMongoClient 获取mongodb客户端
     *
     * @param procNameAndBatchNoAndDsName
     * @return MongoClient
     * @desc 获取mongodb客户端
     */
    public static MongoClient getMongoClient(String procNameAndBatchNoAndDsName) {
        return mongoClientMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * close 关闭mongodb客户端
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 关闭mongodb客户端
     */
    public static void close(String procNameAndBatchNoAndDsName) {
        if (!mongoClientMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            mongoClientMap.get(procNameAndBatchNoAndDsName).close();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            mongoClientMap.remove(procNameAndBatchNoAndDsName);
            Log.info(procNameAndBatchNoAndDsName+"链接已关闭");
        }
    }
}
