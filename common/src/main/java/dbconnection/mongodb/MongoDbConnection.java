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
    public static synchronized void createMonoDbClient(String dsName) {
        if (mongoClientMap.containsKey(dsName)) {
            return;
        }
        System.out.println("数据源启动:" + dsName);
        MongoClient mongoClient = MongoClients.create(dsName);
        mongoClientMap.put(dsName, mongoClient);

        System.out.println(dsName + "数据源启动成功");
    }

    public static synchronized void createMonoDbClient(String dsName, Datasource datasource) {
        if (mongoClientMap.containsKey(dsName)) {
            return;
        }
        System.out.println("数据源启动:" + dsName);
        MongoClient mongoClient = MongoClients.create(datasource.getUrl());
        mongoClientMap.put(dsName, mongoClient);

        System.out.println(dsName + "数据源启动成功");
    }

    public static MongoClient getMongoClient(String dsName, Datasource datasource) {
        if (!mongoClientMap.containsKey(dsName)) {
            createMonoDbClient(dsName, datasource);
        }
        return mongoClientMap.get(dsName);
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
            mongoClientMap.remove(dsName);
        }
    }

    public static void main(String[] args) {
        //getMongoClient("mongodb://admin:123456@192.168.3.172:6001/admin?authSource=admin");
        Map<String, Object> map = MySqlConnection.getJdbcTemplate("1").queryForMap("select * from photon.datasource where name='mongodb1' ");

        Datasource datasource = new Datasource();
        datasource.setId(map.get("id").toString());
        datasource.setName(map.get("name").toString());
        datasource.setType(map.get("type").toString());
        datasource.setDsDatabase(map.get("ds_database").toString());
        datasource.setUsername(map.get("username").toString());
        datasource.setPassword(map.get("password").toString());

        datasource.setId(map.get("ip").toString());


        datasource.setUrl(map.get("url").toString());

        datasource.setPort((String) map.get("port"));

        datasource.setDsOption(map.get("ds_option").toString());
        getMongoClient("1",datasource);
    }

}
