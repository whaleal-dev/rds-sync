package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;

import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * @desc: 专门用来处理索引相关的内容 注意区分版本号
 * @author: lhp
 * @time: 2021/11/23 2:27 下午
 */
public class OplogDealIndexTask implements Runnable {
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * oplogMetadata信息
     */
    private final OplogMetadata oplogMetadata;

    private String proName;

    public OplogDealIndexTask(String proName, OplogMetadata oplogMetadata) {
        this.oplogMetadata = oplogMetadata;
        this.mongoClient = MongoDbConnection.getMongoClient(oplogMetadata.getTargetDsName());
        this.proName = proName;
    }

    public void parseCreateIndex_v5(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("createIndexes").toString();
        String indexName = o.get("name").toString();
        BasicDBObject index = new BasicDBObject();
        Document key = (Document) o.get("key");
        for (Map.Entry<String, Object> indexTemp : key.entrySet()) {
            index.append(indexTemp.getKey(), indexTemp.getValue());
        }
        IndexOptions indexOptions = new IndexOptions().name(indexName);
        if (o.get("unique") != null && o.getBoolean("unique")) {
            indexOptions.unique(true);
        }
        if (o.get("hidden") != null && o.getBoolean("hidden")) {
            indexOptions.hidden(true);
        }
        if (o.get("sparse") != null && o.getBoolean("sparse")) {
            indexOptions.sparse(true);
        }
        if (o.get("weights") != null) {
            Document weights = new Document();
            weights.putAll((Document) o.get("weights"));
            indexOptions.weights(weights);
        }
        if (o.get("default_language") != null) {
            indexOptions.defaultLanguage(o.get("default_language").toString());
        }
        if (o.get("language_override") != null) {
            indexOptions.languageOverride(o.get("language_override").toString());
        }
        if (o.get("min") != null) {
            indexOptions.min(o.getDouble("min"));
        }
        if (o.get("max") != null) {
            indexOptions.max(o.getDouble("max"));
        }
        if (o.get("bucketSize") != null) {
            indexOptions.bucketSize(o.getDouble("bucketSize"));
        }
        if (o.get("expireAfterSeconds") != null) {
            indexOptions.expireAfter(o.getDouble("expireAfterSeconds").longValue(), TimeUnit.SECONDS);
        }
        if (o.get("partialFilterExpression") != null) {
            indexOptions.partialFilterExpression((Document) o.get("partialFilterExpression"));
        }
        if (o.get("collation") != null) {
            Collation.Builder collationBuilder = Collation.builder();
            Document collation = (Document) o.get("collation");
            if (collation.get("locale") != null) {
                collationBuilder.locale(collation.getString("locale"));
            }
            if (collation.get("caseLevel") != null) {
                collationBuilder.caseLevel(collation.getBoolean("caseLevel"));
            }
            if (collation.get("caseFirst") != null) {
                collationBuilder.collationCaseFirst(CollationCaseFirst.fromString(collation.getString("caseFirst")));
            }
            if (collation.get("strength") != null) {
                collationBuilder.collationStrength(CollationStrength.fromInt(collation.getDouble("strength").intValue()));
            }
            if (collation.get("numericOrdering") != null) {
                collationBuilder.numericOrdering(collation.getBoolean("numericOrdering"));
            }
            if (collation.get("alternate") != null) {
                collationBuilder.collationAlternate(CollationAlternate.fromString(collation.getString("alternate")));
            }
            if (collation.get("maxVariable") != null) {
                collationBuilder.collationMaxVariable(CollationMaxVariable.fromString(collation.getString("maxVariable")));
            }
            if (collation.get("normalization") != null) {
                collationBuilder.normalization(collation.getBoolean("normalization"));
            }
            if (collation.get("backwards") != null) {
                collationBuilder.backwards(collation.getBoolean("backwards"));
            }
            indexOptions.collation(collationBuilder.build());
        }


        {
            // 校验当前这批数据是否应该被应用
            String dbTableInfo = oplogMetadata.dbTableCreateOrDropTimeMap.get(dbName + "." + tableName);
            // 若dbTableInfo==null 代表该批数据在传输之前已经存在于目标数据库中
            if (dbTableInfo != null) {
                if (dbTableInfo.startsWith("create")) {
                    long dbCreateTime = Long.parseLong(dbTableInfo.split("_")[1]);
                    BsonTimestamp bsonTimestamp = (BsonTimestamp) document.get("ts");
                    long oplogTime = (long) (bsonTimestamp.getTime() * 1000);
                    //   表的创建时间小于当前这批数据的时间 则丢弃这批数据
                    if (dbCreateTime < oplogTime) {
                        return;
                    }
                } else if (dbTableInfo.startsWith("drop")) {
                    // 表已经被删除了 则丢弃这批数据
                    return;
                }
            }
        }
        try {
            // 要建索引,则必先把之前的索引删除了
            mongoClient.getDatabase(dbName).getCollection(tableName).dropIndex(indexName);
        } catch (Exception ignored) {
        }
        mongoClient.getDatabase(dbName).getCollection(tableName).createIndex(index, indexOptions);
        // 再次检查该表是否存在,若已经不存在了,则进行删除该索引
        {
            // 校验当前这批数据是否应该被应用
            String dbTableInfo = oplogMetadata.dbTableCreateOrDropTimeMap.get(dbName + "." + tableName);
            // 若dbTableInfo==null 代表该批数据在传输之前已经存在于目标数据库中
            if (dbTableInfo != null && dbTableInfo.startsWith("drop")) {
                // 表已经被删除了,则丢弃这批数据
                mongoClient.getDatabase(dbName).getCollection(tableName).dropIndex(indexName);
            }
        }
    }


    /**
     * parseDropIndex 解析删除索引
     *
     * @param document oplog数据
     * @desc 解析删除索引
     */
    public void parseDropIndex(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("dropIndexes").toString();
        String indexName = o.get("index").toString();
        mongoClient.getDatabase(dbName).getCollection(tableName).dropIndex(indexName);
    }

    @Override
    public void run() {
        while (true) {
            long lastCreateOrDropIndexTime = oplogMetadata.createOrDropIndexTime;
            Iterator<Map.Entry<String, Document>> entryIterator = oplogMetadata.dbTableIndexMap.entrySet().iterator();
            while (entryIterator.hasNext()) {
                try {
                    Map.Entry<String, Document> next = entryIterator.next();
                    // 表名
                    String dbTableNameAndIndexName = next.getKey();
                    // 加'锁',每个数据k最多同时有一个线程解析
                    boolean pre = oplogMetadata.dbTableIndexIsUserMap.get(dbTableNameAndIndexName).get();
                    // cas操作
                    if (!pre && oplogMetadata.dbTableIndexIsUserMap.get(dbTableNameAndIndexName).compareAndSet(false, true)) {
                        Document document = next.getValue();
                        if (document == null) {
                            // 释放该表的"锁"
                            oplogMetadata.dbTableIndexIsUserMap.get(dbTableNameAndIndexName).set(false);
                            continue;
                        }
                        Document o = (Document) document.get("o");
                        BsonTimestamp bsonTimestamp = (BsonTimestamp) document.get("ts");
                        if (o.get("dropIndexes") != null) {
                            parseDropIndex(document);
                        } else {
                            parseCreateIndex_v5(document);
                        }
                        // 该同名索引没有发生改变
                        if (bsonTimestamp.compareTo((BsonTimestamp) next.getValue().get("ts")) == 0) {
                            // dcl处理
                            synchronized (oplogMetadata.syncObject) {
                                if (bsonTimestamp.compareTo((BsonTimestamp) next.getValue().get("ts")) == 0) {
                                    oplogMetadata.dbTableIndexIsUserMap.remove(dbTableNameAndIndexName);
                                    entryIterator.remove();
                                }
                            }
                        } else {
                            // 释放该表的"锁"
                            oplogMetadata.dbTableIndexIsUserMap.get(dbTableNameAndIndexName).set(false);
                        }
                    }
                } catch (Exception e) {
                    Log.error("程序:" + proName + ",创建或删除INDEX时发生错误,错误信息:" + e.getMessage());
                }
            }
            // 发生了dll事件,则再次轮训
            if (lastCreateOrDropIndexTime == oplogMetadata.createOrDropIndexTime) {
                // ddl处理完成后 则进行睡眠
                synchronized (oplogMetadata.syncObject) {
                    try {
                        oplogMetadata.syncObject.wait();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}












