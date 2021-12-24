package com.whaleal.photon.core.dbconnection.mongodb;


import com.mongodb.MongoSecurityException;
import com.mongodb.MongoTimeoutException;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;


import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.util.Log;
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
    private static final Map<String, MongoClient> MONGO_CLIENT_MAP = new ConcurrentHashMap<>();

    /**
     * createMonoDbDataBase 创造mongodb客户端
     *
     * @param dsName
     * @desc 创造mongodb客户端。dcl检查
     */
    public static void createMonoDbClient(String dsName, Datasource datasource) {
        if (MONGO_CLIENT_MAP.containsKey(dsName)) {
            return;
        }
        MongoClient mongoClient = MongoClients.create(datasource.getUrl());
        MONGO_CLIENT_MAP.put(dsName, mongoClient);
    }

    /**
     * getMongoClient 获取mongodb客户端
     *
     * @param dsName
     * @return MongoClient
     * @desc 获取mongodb客户端
     */
    public static MongoClient getMongoClient(String dsName) {
        return MONGO_CLIENT_MAP.get(dsName);
    }

    /**
     * close 关闭mongodb客户端
     *
     * @param dsName
     * @desc 关闭mongodb客户端
     */
    public static void close(String dsName) {
        if (!MONGO_CLIENT_MAP.containsKey(dsName)) {
            return;
        }
        try {
            MONGO_CLIENT_MAP.get(dsName).close();
        } catch (Exception e) {
            Log.error("关闭Mongodb客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            MONGO_CLIENT_MAP.remove(dsName);
            Log.info("成功关闭mongodb链接:" + dsName);
        }
    }

    public static boolean checkDsIsCorrect(String url) {
        try {
            MongoClient mongoClient = MongoClients.create(url);
            Document serverStatus = mongoClient.getDatabase("test").runCommand(new Document().append("serverBuildInfo", 1));
            Log.info("MongodbVersion:" + serverStatus.get("version").toString());
        } catch (Exception e) {
            if (e instanceof MongoSecurityException) {
                Log.info("校验mongodbUrl是否正确链接时发生异常,错误信息:" + e.getMessage());
                return false;
            }
            if (e instanceof MongoTimeoutException) {
                Log.info("校验mongodbUrl是否正确链接时发生异常,错误信息:" + e.getMessage());
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        checkDsIsCorrect("");
    }

}
