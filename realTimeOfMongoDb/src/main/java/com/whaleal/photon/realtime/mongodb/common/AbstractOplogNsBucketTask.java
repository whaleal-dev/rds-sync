package com.whaleal.photon.realtime.mongodb.common;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;

import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.thread.RealTimeTaskPoolManager;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: lhp
 * @time: 2021/7/30 11:07 上午
 * @desc: 多个线程操作 此时要处理多个ns。每个ns，最多同时有一个线程处理。
 */
public abstract class AbstractOplogNsBucketTask implements Runnable, ParseOplogInterface {


    public AbstractOplogNsBucketTask(OplogMetadata oplogMetadata, String proName) {
        this.oplogMetadata = oplogMetadata;
        this.sourceDsName = oplogMetadata.sourceDsName;
        this.targetDsName = oplogMetadata.targetDsName;
        this.maxBucketNum = oplogMetadata.getMaxBucketNum();
        this.ddlList = oplogMetadata.getDdlList();
        this.proName = proName;
        this.mongoClient = MongoDbConnection.getMongoClient(this.targetDsName);
    }

    public void parseDDL(Document document) {
        Document o = (Document) document.get("o");
        if (o.get(DROPTABLE) != null && ddlList.contains(DROPTABLE)) {
            writeDataBeforeDDL();
            Log.warn("程序:" + proName + ",发生删表操作:" + document.toJson());
            parseDropTable(document);
        } else if (o.get(CREATETABLE) != null && ddlList.contains(CREATETABLE)) {
            writeDataBeforeDDL();
            Log.warn("程序:" + proName + ",发生建表操作:" + document.toJson());
            parseCreateTable(document);
        } else if (o.get(CREATEINDEX) != null && ddlList.contains(CREATEINDEX)) {
            Log.warn("程序:" + proName + ",发生建索引操作:" + document.toJson());
            parseCreateIndex(document);
        } else if (o.get(COMMITINDEXBUILD) != null && ddlList.contains(CREATEINDEX)) {
            // 5.0的新的见索引方式
            Log.warn("程序:" + proName + ",发生建索引操作:" + document.toJson());
            parseCommitIndexBuild(document);
        } else if (o.get(DROPINDEX) != null && ddlList.contains(DROPINDEX)) {
            Log.warn("程序:" + proName + ",发生删索引操作:" + document.toJson());
            parseDropIndex(document);
        } else if (o.get(RENAMECOLLECTON) != null && ddlList.contains(RENAMECOLLECTON)) {
            writeDataBeforeDDL();
            Log.warn("程序:" + proName + ",发生表重命名操作:" + document.toJson());
            parseRenameTable(document);
        } else if (o.get(CONVERTOCAPPED) != null && ddlList.contains(CONVERTOCAPPED)) {
            Log.warn("程序:" + proName + ",发生设置表上限操作:" + document.toJson());
            parseConvertToCapped(document);
        } else if (o.get(DROPDATABASE) != null && ddlList.contains(DROPDATABASE)) {
            Log.warn("程序:" + proName + ",发生删库操作:" + document.toJson());
            parseDropDatabase(document);
        }
    }

    /**
     * oplog元数据库
     */
    protected final OplogMetadata oplogMetadata;
    /**
     * 源数据源名称
     */
    protected final String sourceDsName;
    /**
     * 目标数据源名称
     */
    protected final String targetDsName;
    /**
     * k桶号 默认[0-16)
     * v为Set<id>
     */
    protected final Map<Integer, Set<String>> bucketSetMap = new HashMap<>();
    /**
     * k桶号 默认[0-16)
     * v为解析好的WriteModel数据集合
     */
    protected final Map<Integer, List<WriteModel<Document>>> bucketWriteModelListMap = new HashMap<>();
    /**
     * mongoClient
     */
    protected final MongoClient mongoClient;
    /**
     * 当前解析的表
     */
    protected String currentDbTable;
    /**
     * 桶个数
     */
    protected final int maxBucketNum;
    /**
     * 要同步的DDL列表
     */
    protected final Set<String> ddlList;

    protected final String proName;

    @Override
    public void run() {
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_NS_BUCKET, 1);
        int IdlingTimes = 0;
        while (true) {
            try {
                /**
                 * 保存每个表的document
                 * k为表名，v为ns解析后的Document
                 */
                Iterator<Map.Entry<String, BlockingQueue<Document>>> entryIterator = oplogMetadata.dbTableQueueOfNsMap.entrySet().iterator();
                if (IdlingTimes++ > 10) {
                    // 10次都没有获取到oplog信息,则进行睡眠
                    TimeUnit.SECONDS.sleep(1);
                    // 10次都没有获得锁 更有可能继续无法获得'锁'
                    IdlingTimes = 8;

                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.REAL_TIME_RUN) {
                        // 此线程需要停止了
                        if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                            if (oplogMetadata.dbTableQueueOfNsDataNum() == 0 && oplogMetadata.documentQueueOfOplog.size() == 0) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入STOP状态,oplog分桶线程即将关闭");
                                break;
                            }
                        } else if (proStatus == ProStatus.FULL_SYNC_RUN_AND_REAL_TIME_SLEEP || proStatus == ProStatus.REAL_TIME_SLEEP) {
                            if (oplogMetadata.dbTableQueueOfNsDataNum() != 0 || oplogMetadata.documentQueueOfOplog.size() != 0) {
                                continue;
                            }
                            // 此线程需要进行睡眠
                            final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                            synchronized (proRealTimeObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,oplog分桶线程即将睡眠");
                                proRealTimeObjectLock.wait();
                            }
                        }
                    }

                }
                while (entryIterator.hasNext()) {
                    Map.Entry<String, BlockingQueue<Document>> next = entryIterator.next();
                    // 表名
                    String dbTableName = next.getKey();
                    // 当前线程要操作的表名进行赋值
                    this.currentDbTable = dbTableName;
                    // 加'锁',每个数据表最多同时有一个线程解析
                    boolean pre = oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).get();
                    // cas操作
                    if (!pre && oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).compareAndSet(false, true)) {
                        // 队列数据
                        Queue<Document> documentQueue = next.getValue();
                        // 该表内无数据
                        if (documentQueue.size() == 0) {
                            IdlingTimes++;
                            // 释放'锁'
                            oplogMetadata.dbTableIsUseOfNsBucket.get(dbTableName).set(false);
                            continue;
                        }
                        // 该表内有数据,IdlingTimes轮转次数设为0
                        IdlingTimes = 0;
                        // 对该表的bucketSetMap,bucketWriteModelListMap进行重新赋值
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
                Log.error("程序:" + proName + ",oplog的分桶线程发生错误,错误信息:" + e.getMessage());
            }
        }
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_NS_BUCKET, -1);
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

    /**
     * putDataToCache
     *
     * @param ns
     * @param bucketNum
     * @desc 添加数据到下一层级
     */
    public void putDataToCache(String ns, int bucketNum) {
        try {
            // 16个公共区
            int nsBucketNum = Math.abs((ns + bucketNum).hashCode() % oplogMetadata.getMaxBucketNum());
            BatchDataEntityOfMongodb batchDataEntity = new BatchDataEntityOfMongodb();
            batchDataEntity.setDbTableName(ns);
            batchDataEntity.setDataList(bucketWriteModelListMap.get(bucketNum));
            batchDataEntity.setSourceDsName(sourceDsName);
            oplogMetadata.dbTableBucketBatchDataQueueMap.get(nsBucketNum).put(batchDataEntity);
            // 修改map中的信息
            bucketSetMap.put(bucketNum, new HashSet<>());
            bucketWriteModelListMap.put(bucketNum, new ArrayList());
        } catch (Exception e) {
            Log.error("程序:" + proName + "添加数据到oplogWrite线程时发生异常,错误信息:" + e.getMessage());
        }
    }

    /**
     * init
     *
     * @desc 初始化该表的信息
     */
    public void init() {
        for (int i = 0; i < oplogMetadata.getMaxBucketNum(); i++) {
            bucketSetMap.put(i, new HashSet<>());
            bucketWriteModelListMap.put(i, new ArrayList());
        }
    }

    /**
     * com.whaleal.photon.source.mongodb.parse
     *
     * @param documentQueue
     * @desc 解析Document
     */
    public void parse(Queue<Document> documentQueue) {
        int parseSize = 0;
        while (true) {
            try {
                Document document = documentQueue.poll();
                // 当队列数据为0,或者当前表已经处理了8096条数据
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
                    parseDDL(document);
                    oplogMetadata.bulkWriteInfo.get("cmd").add(1);
                }
                if (parseSize++ > oplogMetadata.getMaxTableQueueSizeOfNs()) {
                    break;
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + "解析oplog日志时发生异常,错误信息:" + e.getMessage());
            }
        }
    }


    /**
     * parseCreateIndex 解析建立索引
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public abstract void parseCommitIndexBuild(Document document);

    /**
     * parseDropTable 解析删表
     *
     * @param document oplog数据
     * @desc 解析删表
     */

    /**
     * writeDataBeforeDDL 等待桶数据的写完
     *
     * @desc 等待桶数据的写完
     */
    public void writeDataBeforeDDL() {
        // 把数据推到下一层级
        putDataToCache();
        // k为桶号,v为该桶数据被处理完的次数
        Map<Integer, Integer> dbTableBucketIsEmptyCountTemp = new HashMap<>();
        // 记录已经没有处理完的桶
        Set<Integer> bucketSet = new HashSet<>();
        for (int bucketNum = 0; bucketNum < oplogMetadata.getMaxBucketNum(); bucketNum++) {
            //记录上次次数
            dbTableBucketIsEmptyCountTemp.put(bucketNum, oplogMetadata.dbTableBucketIsEmptyCount.get(bucketNum).get());
            bucketSet.add(bucketNum);
        }
        // 重点
        // 此方法判断前序数据是否完成写入
        long startWaitTime = System.currentTimeMillis();
        while (true) {
            Iterator<Integer> iterator = bucketSet.iterator();
            while (iterator.hasNext()) {
                Integer bucketNum = iterator.next();
                int currentBucketIsEmptyCount = oplogMetadata.dbTableBucketIsEmptyCount.get(bucketNum).get();
                int oldBucketIsEmptyCount = dbTableBucketIsEmptyCountTemp.get(bucketNum);
                if (currentBucketIsEmptyCount - oldBucketIsEmptyCount >= 2) {
                    iterator.remove();
                    continue;
                }
                if (currentBucketIsEmptyCount < oldBucketIsEmptyCount) {
                    // 发生了currentBucketIsEmptyCount重计数,需要对oldBucketIsEmptyCount重新赋值
                    dbTableBucketIsEmptyCountTemp.put(bucketNum, currentBucketIsEmptyCount);
                }
            }
            if (bucketSet.size() == 0) {
                break;
            }
            long currentTime = System.currentTimeMillis();
            // 最多等待某个轮训操作10s
            if (currentTime - startWaitTime > 10000) {
                break;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    // 无需处理
                }
            }
        }
    }

    @Override
    public void parseDropTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("drop").toString();
        mongoClient.getDatabase(dbName).getCollection(tableName).drop();
        // 记录该表删除的时间
        oplogMetadata.dbTableCreateOrDropTimeMap.put(dbName + "." + tableName, "drop_" + System.currentTimeMillis());
        if (oplogMetadata.dbTableQueueOfNsMap.get(dbName + "." + tableName).size() == 0) {
            synchronized (oplogMetadata.syncObject) {
                // dcl双重检查
                if (oplogMetadata.dbTableQueueOfNsMap.get(dbName + "." + tableName).size() == 0) {
                    oplogMetadata.dbTableIsUseOfNsBucket.remove(dbName + "." + tableName);
                    oplogMetadata.dbTableQueueOfNsMap.remove(dbName + "." + tableName);
                }
            }
        }
    }

    /**
     * parseCreateTable 解析创建表
     *
     * @param document oplog数据
     * @desc 解析创建表
     */
    @Override
    public void parseCreateTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("create").toString();
        oplogMetadata.dbTableCreateOrDropTimeMap.put(dbName + "." + tableName, "drop_" + System.currentTimeMillis());
        CreateCollectionOptions collectionOptions = new CreateCollectionOptions();
        // 上限集合
        if (o.get("capped") != null && "true".equals(o.get("capped").toString())) {
            collectionOptions.capped(true);
        }
        // 最大内存量
        if (o.get("size") != null) {
            long size = Long.parseLong(o.get("size").toString());
            collectionOptions.sizeInBytes(size);
        }
        // 最大数据条数
        if (o.get("max") != null) {
            long max = Long.parseLong(o.get("max").toString());
            collectionOptions.maxDocuments(max);
        }
        // 整理规则
        if (o.get("locale") != null) {
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
                collationBuilder.collationStrength(CollationStrength.fromInt(collation.getInteger("strength")));
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
            collectionOptions.collation(collationBuilder.build());
        }
        // 建表前已经删表
        mongoClient.getDatabase(dbName).getCollection(tableName).drop();
        // 正式建表
        mongoClient.getDatabase(dbName).createCollection(tableName, collectionOptions);
        // 记录该表创建的时间
        oplogMetadata.dbTableCreateOrDropTimeMap.put(dbName + "." + tableName, "create_" + System.currentTimeMillis());
    }


    /**
     * parseRenameTable 解析表重命名
     *
     * @param document oplog数据
     * @desc 解析表重命名
     */
    @Override
    public void parseRenameTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String dbNameAndTableName = o.get("renameCollection").toString();
        String tableName = dbNameAndTableName.split("\\.")[1];
        String newTableName = o.get("to").toString().split("\\.")[1];
        MongoNamespace mongoNamespace = new MongoNamespace(dbName + "." + newTableName);
        RenameCollectionOptions renameCollectionOptions = new RenameCollectionOptions();
        renameCollectionOptions.dropTarget(false);
        // 如果目标表已经存在,是否进行删除
        if (o.get("dropTarget") != null && o.getBoolean("dropTarget")) {
            renameCollectionOptions.dropTarget(true);
        }
        mongoClient.getDatabase(dbName).getCollection(tableName).renameCollection(mongoNamespace, renameCollectionOptions);
        // 新表的创建时间
        oplogMetadata.dbTableCreateOrDropTimeMap.put(dbName + "." + newTableName, "create_" + System.currentTimeMillis());
        // 记录该表删除的时间
        oplogMetadata.dbTableCreateOrDropTimeMap.put(dbName + "." + tableName, "drop_" + System.currentTimeMillis());
        if (oplogMetadata.dbTableQueueOfNsMap.get(dbName + "." + tableName).size() == 0) {
            synchronized (oplogMetadata.syncObject) {
                // dcl双重检查
                if (oplogMetadata.dbTableQueueOfNsMap.get(dbName + "." + tableName).size() == 0) {
                    oplogMetadata.dbTableIsUseOfNsBucket.remove(dbName + "." + tableName);
                    oplogMetadata.dbTableQueueOfNsMap.remove(dbName + "." + tableName);
                }
            }
        }
    }


    /**
     * parseCreateIndex 解析建立索引
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    @Override
    public void parseCreateIndex(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("createIndexes").toString();
        String indexName = o.get("name").toString();
        synchronized (oplogMetadata.syncObject) {
            try {
                oplogMetadata.createOrDropIndexTime = System.currentTimeMillis();
                if (!oplogMetadata.dbTableIndexIsUserMap.containsKey(dbName + "." + tableName + ".." + indexName)) {
                    oplogMetadata.dbTableIndexIsUserMap.put(dbName + "." + tableName + ".." + indexName, new AtomicBoolean());
                }
                oplogMetadata.dbTableIndexMap.put(dbName + "." + tableName + ".." + indexName, document);
                oplogMetadata.syncObject.notifyAll();
            } catch (Exception e) {
                // 报错无需处理
            }
        }
    }

    /**
     * parseDropIndex 解析删除索引
     *
     * @param document oplog数据
     * @desc 解析删除索引
     */
    @Override
    public void parseDropIndex(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("dropIndexes").toString();
        String indexName = o.get("index").toString();
        synchronized (oplogMetadata.syncObject) {
            try {
                oplogMetadata.createOrDropIndexTime = System.currentTimeMillis();
                if (!oplogMetadata.dbTableIndexIsUserMap.containsKey(dbName + "." + tableName + ".." + indexName)) {
                    oplogMetadata.dbTableIndexIsUserMap.put(dbName + "." + tableName + ".." + indexName, new AtomicBoolean());
                }
                oplogMetadata.dbTableIndexMap.put(dbName + "." + tableName + ".." + indexName, document);
                oplogMetadata.syncObject.notifyAll();
            } catch (Exception e) {
                // 报错无需处理
            }
        }
    }

    /**
     * parseInsert 解析插入数据
     *
     * @param document oplog数据
     * @desc 解析插入数据
     */
    @Override
    public void parseInsert(Document document) {
        String _id = ((Document) document.get("o")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % oplogMetadata.getMaxBucketNum());
        // 检查该桶bucketSetMap是否存在。若不存在 则添加
        if (!bucketSetMap.get(bucketNum).add(_id)) {
            putDataToCache(currentDbTable, bucketNum);
            bucketSetMap.get(bucketNum).add(_id);
        }
        Document insertDocument = (Document) document.get("o");
        // fromMigrate要特殊处理      shared准备
        if (document.get("fromMigrate") == null || !document.getBoolean("fromMigrate")) {
            bucketWriteModelListMap.get(bucketNum).add(new InsertOneModel<Document>(insertDocument));
        } else {
            // 是否开启upsert
            // 为shared准备
            ReplaceOptions option = new ReplaceOptions();
            option.upsert(true);
            bucketWriteModelListMap.get(bucketNum).add(new ReplaceOneModel<Document>(insertDocument, insertDocument, option));
        }
    }

    /**
     * parseDelete 解析删除数据
     *
     * @param document oplog数据
     * @desc 解析删除数据
     */
    @Override
    public void parseDelete(Document document) {
        String _id = ((Document) document.get("o")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % oplogMetadata.getMaxBucketNum());
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
    }

    /**
     * parseDelete 解析删除数据
     *
     * @param document oplog数据
     * @desc 解析删除数据
     */
    public void parseConvertToCapped(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("convertToCapped").toString();
        mongoClient.getDatabase(dbName).getCollection(tableName);
        // 暂时不处理
    }

    /**
     * parseDropDatabase 删库
     *
     * @param document oplog数据
     * @desc 删库。
     */
    @Override
    public void parseDropDatabase(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];

        mongoClient.getDatabase(dbName).drop();
        Iterator<Map.Entry<String, AtomicBoolean>> iterator = oplogMetadata.dbTableIsUseOfNsBucket.entrySet().iterator();
        while (iterator.hasNext()) {
            try {
                // 记录该表删除的时间
                String dbTableName = iterator.next().getKey();
                String[] split = dbTableName.split("\\.", 2);
                if (!split[0].equals(dbName)) {
                    continue;
                }
                oplogMetadata.dbTableCreateOrDropTimeMap.put(dbTableName, "drop_" + System.currentTimeMillis());
                if (oplogMetadata.dbTableQueueOfNsMap.get(dbTableName).size() == 0) {
                    synchronized (oplogMetadata.syncObject) {
                        // dcl双重检查
                        if (oplogMetadata.dbTableQueueOfNsMap.get(dbTableName).size() == 0) {
                            oplogMetadata.dbTableIsUseOfNsBucket.remove(dbTableName);
                            oplogMetadata.dbTableQueueOfNsMap.remove(dbTableName);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
