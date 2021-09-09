package task;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;
import common.OplogMetadata;
import common.dataclass.BatchDataEntity;
import dbconnection.mongodb.MongoDbConnection;
import org.bson.Document;
import util.Log;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: lhp
 * @time: 2021/7/30 11:07 上午
 * @desc: 多个线程操作 此时要处理多个ns。每个ns，最多同时有一个线程处理。
 */
public class OplogNsBucketTask implements Runnable {
    /**
     * 数据源名称
     */
    public String sourceDsName;
    /**
     * 目标数据源
     */
    public String targetDsName;
    /**
     * oplog元数据库类
     */
    private OplogMetadata oplogMetadata;
    /**
     * k桶号
     * v为Set
     */
    private Map<Integer, Set<String>> bucketSetMap = new HashMap<>();
    /**
     * k桶号
     * v为解析好的WriteModel数据集合
     */
    private Map<Integer, ArrayList> bucketWriteModelListMap = new HashMap<>();
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 当前解析的表
     */
    private String currentDbTable;
    /**
     * 桶个数
     */
    private int cacheSize;

    public OplogNsBucketTask(OplogMetadata oplogMetadata, int cacheSize) {
        this.cacheSize = cacheSize;
        this.oplogMetadata = oplogMetadata;
        this.sourceDsName = oplogMetadata.sourceDsName;
        this.targetDsName = oplogMetadata.targetDsName;
        this.mongoClient = MongoDbConnection.getMongoClient(oplogMetadata.procNameAndBatchNo+targetDsName);
        System.out.println("OplogNsBucketTask");
    }

    @Override
    public void run() {
        int IdlingTimes = 0;
        while (true) {
            try {
                /**
                 * 保存每个表的document
                 * k为表名，v为ns解析后的Document
                 */
                Iterator<Map.Entry<String, BlockingQueue<Document>>> entryIterator = oplogMetadata.dbTableQueueMap.entrySet().iterator();
                if (IdlingTimes++ > 20) {
                    TimeUnit.SECONDS.sleep(1);
                    //  Log.info(sourceDsName + ",OplogNsBucketTaskWait");
                    IdlingTimes = 19;
                }
                while (entryIterator.hasNext()) {
                    Map.Entry<String, BlockingQueue<Document>> next = entryIterator.next();
                    // 表名
                    String dbTableName = next.getKey();
                    // 当前线程要操作的表名进行赋值
                    currentDbTable = dbTableName;
                    // 加'锁',每个数据表最多同时有一个线程解析
                    boolean pre = oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).get();
                    if (!pre && oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).compareAndSet(false, true)) {
                        // 队列数据
                        Queue<Document> documentQueue = next.getValue();
                        if (documentQueue.size() == 0) {
                            IdlingTimes++;
                            oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).set(false);
                            continue;
                        }
                        Log.info(sourceDsName + ",解析OplogNsBucketTask表:" + dbTableName);
                        IdlingTimes = 0;
                        // 对该表的bucketSetMap，bucketWriteModelListMap进行重新赋值
                        init();
                        // 解析队列中的数据
                        parse(documentQueue);
                        // 解析后把数据放入下一层级
                        putDataToCache();
                        // 释放该表的"锁"
                        oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).set(false);
                    }
                }
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }


    /**
     * init
     *
     * @desc 初始化该表的信息
     */
    public void init() {
        for (int i = 0; i < cacheSize; i++) {
            bucketSetMap.put(i, new HashSet<>());
            bucketWriteModelListMap.put(i, new ArrayList());
        }
    }

    /**
     * parse
     *
     * @param documentQueue
     * @desc 解析Document
     */
    public void parse(Queue<Document> documentQueue) {
        int parseSize = 0;
        while (true) {
            try {
                Document document = documentQueue.poll();
                // 当队列数据为0，或者当前表已经处理了2000条数据
                if (document == null) {
                    break;
                }
                String op = document.get("op").toString();
                if ("i".equals(op)) {
                    parseInsert(document);
                } else if ("u".equals(op)) {
                    parseUpdate(document);
                } else if ("d".equals(op)) {
                    parseDelete(document);
                } else if ("c".equals(op)) {
                    Log.info("sourceDsName:" + sourceDsName + ",出现了DDL:" + document.toJson());
                    parseDDL(document);
                }
                if (parseSize++ > oplogMetadata.maxTableQueueSize) {
                    break;
                }
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    /**
     * parseDDL
     *
     * @param document
     * @desc 解析DDL。ddl数据直接在本线程内执行
     */
    public void parseDDL(Document document) {
        Document o = (Document) document.get("o");
        if (o.get("drop") != null) {
            writeDataBeforeDDL();
            parseDropTable(document);
        } else if (o.get("renameCollection") != null) {
            writeDataBeforeDDL();
            parseRenameTable(document);
        } else if (o.get("createIndexes") != null) {
            // 对于删除索引和建立索引,不需要严格保持顺序问题。
            parseCreateIndex(document);
        } else if (o.get("dropIndexes") != null) {
            parseDropIndex(document);
        }
    }

    /**
     * parseDropTable 解析删表
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public void parseDropTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("drop").toString();
        mongoClient.getDatabase(dbName).getCollection(tableName).drop();
    }

    public void writeDataBeforeDDL() {
        putDataToCache();
        for (int i = 0; i < cacheSize; i++) {
            while (true) {
                String dbTableBucketNum = currentDbTable + "_" + i;
                boolean pre = oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).get();
                if (!pre && oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).compareAndSet(false, true)) {
                    Queue<BatchDataEntity> batchDataEntities = oplogMetadata.dbTableBucketBatchDataQueueMap.get(dbTableBucketNum);
                    write(batchDataEntities);
                    oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).set(false);
                    break;
                }
            }
        }
    }

    /**
     * write
     *
     * @param documentQueue
     * @desc 执行写入
     */
    public void write(Queue<BatchDataEntity> documentQueue) {
        while (true) {
            try {
                BatchDataEntity batchDataEntity = documentQueue.poll();
                if (batchDataEntity == null) {
                    break;
                }
                bulkExecute(batchDataEntity);
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    /**
     * bulkExecute 批量写数据
     *
     * @desc 批量写数据
     */
    public void bulkExecute(BatchDataEntity batchDataEntity) {
        try {
            List list = batchDataEntity.getDataList();
            if (list.size() == 0) {
                return;
            }
            String dbTableName = batchDataEntity.getDbTableName();
            String dbName = dbTableName.split("\\.", 2)[0];
            String tableName = dbTableName.split("\\.", 2)[1];
            BulkWriteResult bulkWriteResult = this.mongoClient.getDatabase(dbName).
                    getCollection(tableName).bulkWrite(list, new BulkWriteOptions().ordered(false));
        } catch (Exception e) {
            Log.error(e.getMessage());
            try {
                List list = batchDataEntity.getDataList();
                if (list.size() == 0) {
                    return;
                }
                String dbTableName = batchDataEntity.getDbTableName();
                String dbName = dbTableName.split("\\.", 2)[0];
                String tableName = dbTableName.split("\\.", 2)[1];
                BulkWriteResult bulkWriteResult = this.mongoClient.getDatabase(dbName).
                        getCollection(tableName).bulkWrite(list, new BulkWriteOptions().ordered(false));
            } catch (Exception exception) {
            }
        }
    }

    /**
     * parseRenameTable 解析表重命名
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public void parseRenameTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String dbNameAndTableName = o.get("renameCollection").toString();
        String tableName = dbNameAndTableName.split("\\.")[1];
        String newTableName = o.get("to").toString().split("\\.")[1];
        MongoNamespace mongoNamespace = new MongoNamespace(dbName + "." + newTableName);
        mongoClient.getDatabase(dbName).getCollection(tableName).renameCollection(mongoNamespace);
    }

    /**
     * parseCreateIndex 解析建立索引
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public void parseCreateIndex(Document document) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
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
                if (o.get("unique") != null) {
                    indexOptions.unique(true);
                }
                mongoClient.getDatabase(dbName).getCollection(tableName).createIndex(index, indexOptions);
            }
        };
        new Thread(runnable).start();
    }

    /**
     * parseDropIndex 解析删除索引
     *
     * @param document oplog数据
     * @desc 解析删除索引
     */
    public void parseDropIndex(Document document) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String ns = document.get("ns").toString();
                String[] nsSplit = ns.split("\\.", 2);
                String dbName = nsSplit[0];
                Document o = (Document) document.get("o");
                String tableName = o.get("dropIndexes").toString();
                String indexName = o.get("index").toString();
                mongoClient.getDatabase(dbName).getCollection(tableName).dropIndex(indexName);
            }
        };
        new Thread(runnable).start();
    }

    /**
     * parseInsert 解析插入数据
     *
     * @param document oplog数据
     * @desc 解析插入数据
     */
    public void parseInsert(Document document) {
        String _id = ((Document) document.get("o")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % cacheSize);
        // 检查该桶bucketSetMap是否存在。若不存在 则添加
        if (!bucketSetMap.get(bucketNum).add(_id)) {
            putDataToCache(currentDbTable, bucketNum);
            bucketSetMap.get(bucketNum).add(_id);
        }
        Document insertDocument = (Document) document.get("o");
        // fromMigrate要特殊处理
        if (document.get("fromMigrate") == null || !document.getBoolean("fromMigrate")) {
            bucketWriteModelListMap.get(bucketNum).add(new InsertOneModel<Document>(insertDocument));
        } else {
            // 是否开启upsert
            ReplaceOptions option = new ReplaceOptions();
            option.upsert(true);
            bucketWriteModelListMap.get(bucketNum).add(new ReplaceOneModel<Document>(insertDocument, insertDocument, option));
            // Log.error("ReplaceOneModel:" + document);
        }
    }

    /**
     * parseUpdate 解析更新数据
     *
     * @param document oplog数据
     * @desc 解析更新数据
     */
    public void parseUpdate(Document document) {
        String _id = ((Document) document.get("o2")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % cacheSize);
        // 检查该桶bucketSetMap是否存在。若不存在 则添加
        if (!bucketSetMap.get(bucketNum).add(_id)) {
            putDataToCache(currentDbTable, bucketNum);
            bucketSetMap.get(bucketNum).add(_id);
        }
        Document o2 = ((Document) document.get("o2"));
        Document o = (Document) document.get("o");
        o.remove("$v");
        // 有些oplog的o没有$set和$unset
        if (o.get("$set") == null && o.get("$unset") == null) {
            // 是否开启upsert
            ReplaceOptions option = new ReplaceOptions();
            option.upsert(true);
            bucketWriteModelListMap.get(bucketNum).add(new ReplaceOneModel<Document>(o2, o, option));
        } else {
            bucketWriteModelListMap.get(bucketNum).add(new UpdateOneModel<Document>(o2, o));
        }
    }

    /**
     * parseDelete 解析删除数据
     *
     * @param document oplog数据
     * @desc 解析删除数据
     */
    public void parseDelete(Document document) {
        String _id = ((Document) document.get("o")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % cacheSize);
        // 检查该桶bucketSetMap是否存在。若不存在 则添加
        if (!bucketSetMap.get(bucketNum).add(_id)) {
            putDataToCache(currentDbTable, bucketNum);
            bucketSetMap.get(bucketNum).add(_id);
        }
        Document deleteDocument = (Document) document.get("o");
        DeleteOneModel<Document> deleteOneModel = new DeleteOneModel<Document>(deleteDocument);
        // fromMigrate要特殊处理
        if (document.get("fromMigrate") == null || !document.getBoolean("fromMigrate")) {
            bucketWriteModelListMap.get(bucketNum).add(deleteOneModel);
        }
//        else {
//            Log.error("我是被跳跃的Delete:" + document);
//        }
    }

    /**
     * putDataToCache
     *
     * @param ns
     * @param bucketNum
     * @desc 添加数据到下一层级
     */
    public void putDataToCache(String ns, int bucketNum) {
        try {
            String nsBucketNum = ns + "_" + bucketNum;
            // 判断下一层级dbTableBucketBatchDataQueueMap 是否存在ns + "_" + bucketNum
            if (!oplogMetadata.dbTableBucketIsUseOfWrite.containsKey(nsBucketNum)) {
                // 下一层级dbTableBucketIsUse添加对应的AtomicBoolean
                oplogMetadata.dbTableBucketIsUseOfWrite.put(nsBucketNum, new AtomicBoolean());
                oplogMetadata.dbTableBucketBatchDataQueueMap.put(
                        nsBucketNum,
                        new LinkedBlockingQueue<BatchDataEntity>(oplogMetadata.maxTableBatchNum));
            }
            BatchDataEntity batchDataEntity = new BatchDataEntity();
            batchDataEntity.setDbTableName(ns);
            batchDataEntity.setDataList(bucketWriteModelListMap.get(bucketNum));
            batchDataEntity.setSourceDsName(sourceDsName);
            oplogMetadata.dbTableBucketBatchDataQueueMap.get(nsBucketNum).put(batchDataEntity);
            //修改map中的信息
            bucketSetMap.put(bucketNum, new HashSet<>());
            bucketWriteModelListMap.put(bucketNum, new ArrayList());
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    /**
     * putDataToCache 添加数据到下一层级
     *
     * @desc 添加所有数据到下一层级
     */
    public void putDataToCache() {
        Set<Integer> keySet = bucketWriteModelListMap.keySet();
        for (Integer bucketNum : keySet) {
            putDataToCache(currentDbTable, bucketNum);
        }
    }
}
