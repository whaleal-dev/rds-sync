package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.thread.RealTimeTaskPoolManager;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取oplog中的数据
 */

public class OplogReadTask implements Runnable {

    private final static Document projectionField = new Document();

    static {
        projectionField.put("ts", 1);
        projectionField.put("ns", 1);
        projectionField.put("o", 1);
        projectionField.put("o2", 1);
        projectionField.put("op", 1);
        // shard迁移时 使用该字段
        projectionField.put("fromMigrate", 1);
    }

    /**
     * 程序元信息
     */
    private final ProgramInfo programInfo;
    /**
     * 程序名
     */
    private final String proName;
    /**
     * 源数据源名称
     */
    private final String sourceDsName;
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * 表过滤策略 使用正则表达式进行过滤
     */
    private final String dbTableWhite;
    /**
     * 开始读取该数据源的时间
     */
    private int startTimeOfOplog;
    /**
     * 开始读取该oplog的毫秒
     */
    private int inc = 0;
    /**
     * 延迟时间s
     */
    private int delayTime = 0;
    /**
     * 结束读取该oplog的时间
     */
    private final int endTimeOfOplog;
    /**
     * 是否同步DDL
     */
    private final boolean filterDdl;
    /**
     * oplog元数据库类.保存数据信息的地方
     */
    private final OplogMetadata oplogMetadata;
    /**
     * 本批读取次数
     */
    private int readNum = 0;

    /**
     * 是否读取完成
     */
    private boolean isReadScanOver = false;

    public OplogReadTask(ProgramInfo programInfo, int startTimeOfOplog, int inc, int endTimeOfOplog, OplogMetadata oplogMetadata) {
        this.programInfo = programInfo;
        this.filterDdl = programInfo.isFilterDdl();
        this.dbTableWhite = programInfo.getDbTableWhite();
        this.sourceDsName = programInfo.getSourceDsName();
        this.endTimeOfOplog = endTimeOfOplog;
        this.startTimeOfOplog = startTimeOfOplog;
        this.proName = programInfo.getProName();
        this.inc = inc;
        this.delayTime = programInfo.getDelayTime();
        this.mongoClient = MongoDbConnection.getMongoClient(programInfo.getSourceDsName());
        this.oplogMetadata = oplogMetadata;

    }


    @Override
    public void run() {
        // 当出现异常时 可以进行进行查询
        while (!isReadScanOver) {
            RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_READ, 1);
            Log.info("程序:" + proName + ",准备读取oplog");
            try {
                {
                    // 进行判断此程序的状态
                    Integer proStatus = ProStatus.getProStatus(proName);
                    // 此线程需要停止了
                    if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                        Log.warn("程序:" + proName + ",检测到该实时程序进入STOP状态,oplog读取线程即将停止");
                        return;
                    } else if (proStatus == ProStatus.FULL_SYNC_RUN_AND_REAL_TIME_SLEEP || proStatus == ProStatus.REAL_TIME_SLEEP) {
                        // 此线程需要进行睡眠
                        final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                        synchronized (proRealTimeObjectLock) {
                            Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,oplog读取线程即将睡眠");
                            proRealTimeObjectLock.wait();
                        }
                    }
                }
                BsonTimestamp startTime = new BsonTimestamp(startTimeOfOplog, inc);
                // 获取oplog的最早时间
                BsonTimestamp oplogStartTime = getStartTimeOfOplog();
                Log.info("程序:" + proName + ",检查是否错过滑动窗口时间:{ 程序开始时间:" + startTime + ",oplog.rs最早记录时间:" + oplogStartTime + "}");
                // 如果oplog的开始时间小于startTimeOfReady，即全表同步期间oplog没有被覆盖
                // startTimeOfReady=0时代表为增量抽取数据。抽取范围[minTs,正无穷)
                // 程序开始时间要大小oplog的开始时间 100s
                if ((startTime.getTime() - oplogStartTime.getTime()) > 100 || startTimeOfOplog == 0) {
                    if (delayTime == 0) {
                        source();
                    } else {
                        sourceOfDelay();
                    }
                } else {
                    Log.info("程序:" + proName + ",读取oplog失败:错过滑动窗口时间oplog被覆盖,本程序即将退出");
                    ProStatus.updateProStatus(proName, ProStatus.PRO_STOP);
                    break;
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",读取oplog失败时发生错误,错误信息:" + e.getMessage());
            } finally {
                RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_READ, -1);
            }
        }

    }

    /**
     * getStartTimeOfOplog 获取oplog的开始时间
     *
     * @desc 获取oplog的第一条数据的时间
     */
    public BsonTimestamp getStartTimeOfOplog() {
        while (true) {
            try {
                MongoCollection topicCollection = mongoClient.getDatabase("local").getCollection("oplog.rs");
                Document document = (Document) topicCollection.find().first();
                BsonTimestamp bsonTimestamp = (BsonTimestamp) document.get("ts");
                if (bsonTimestamp.getTime() > 0) {
                    return bsonTimestamp;
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",获取oplog.rs第一条记录时发生错误,错误信息:" + e.getMessage());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception ignored) {
                }
            }
        }
    }


    /**
     * source 读取oplog的中数据
     *
     * @desc 读取oplog的中数据
     */
    public void source() {
        BsonTimestamp docTime = new BsonTimestamp(startTimeOfOplog, inc);
        Log.info("程序:" + proName + ",开始读取oplog的数据");
        BasicDBObject condition = new BasicDBObject();
        if (startTimeOfOplog != 0) {
            // 设置查询数据的时间范围
            condition.append("ts", new Document().append("$gte", docTime));
        }
        if (endTimeOfOplog != 0) {
            // 带有范围的oplog
            condition.append("ts", new Document().append("$gte", docTime).append("&lte", new BsonTimestamp(endTimeOfOplog, 0)));
        }
        Log.info("程序:" + proName + ",读取oplog.rs的条件为:" + condition);
        try {
            MongoCollection oplogCollection = mongoClient.getDatabase("local").getCollection("oplog.rs");
            MongoCursor<Document> cursor =
                    oplogCollection.find(condition).projection(projectionField).
                            sort(new Document("$natural", 1)).
                            cursorType(CursorType.TailableAwait).noCursorTimeout(true).batchSize(8192).iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                String ns = document.get("ns").toString();
                // 记录当前oplog的时间
                oplogMetadata.lastOplogTs = (BsonTimestamp) document.get("ts");

                {
                    // 进行判断此程序的状态
                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.REAL_TIME_RUN) {
                        if (proStatus == ProStatus.FULL_SYNC_RUN_AND_REAL_TIME_SLEEP || proStatus == ProStatus.REAL_TIME_SLEEP) {
                            // 此线程需要进行睡眠
                            final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                            synchronized (proRealTimeObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,oplog读取线程即将睡眠");
                                proRealTimeObjectLock.wait();
                            }
                        } else if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                            // 此线程需要停止了
                            cursor.close();
                        }
                    }
                }
                // 心累 单独为3.2进行判断建立索引
                if (programInfo.getSourceVersion().startsWith("3") && ns.endsWith(".system.indexes")) {
                    // 可能为建立索引
                    Document o = (Document) document.get("o");
                    // 插入数据 但是没有_id 。则是建立索引的oplog
                    if (!o.containsKey("_id")) {
                        // 开始构造通用的建立索引的oplog
                        document.put("op", "c");
                        String dbName = ns.split("\\.", 2)[0];
                        document.put("ns", dbName + ".$cmd");
                        String tableName = o.get("ns").toString().split("\\.", 2)[1];
                        o.put("createIndexes", tableName);
                        o.remove("ns");
                        document.put("o", o);
                        ns = dbName + ".$cmd";
                    }
                }
                // 库表名过滤.
                if (!ns.matches(dbTableWhite)) {
                    // 没有通过库表过滤,则进入判断是否通过ddl判断
                    // cmd的进行下一级步骤进行过滤
                    if (!filterDdl || !document.get("op").equals("c")) {
                        continue;
                    }
                }

                // 过滤元数据库信息 local admin config库数据不同步
                if (!ns.startsWith("local.") && !ns.startsWith("admin.") && !ns.startsWith("config.")) {
                    // 保留本次oplog的ts读取时间
                    docTime = (BsonTimestamp) document.get("ts");
                    if (readNum++ > 1000000) {
                        readNum = 0;
                        oplogMetadata.millionNum++;
                    }
                    Log.info(document.toJson());
                    oplogMetadata.documentQueueOfOplog.put(document);
                }
            }
            // 如果程序能够正常走到这里 则代表查询完毕 更新程序的状态
            ProStatus.updateProStatus(proName, ProStatus.REAL_TIME_STOP);
            isReadScanOver = true;
        } catch (Exception e) {
            isReadScanOver = false;
            // 重新更新查询的开始时间和结束时间
            this.startTimeOfOplog = docTime.getTime();
            this.inc = docTime.getInc();
            Log.error("程序:" + proName + ",读取oplog发现异常,错误信息为:" + e.getMessage());
        }
    }

    public void sourceOfDelay() {
        BsonTimestamp docTime = new BsonTimestamp(startTimeOfOplog, inc);
        Log.info("程序:" + proName + ",开始读取oplog的数据");
        BasicDBObject condition = new BasicDBObject();
        if (startTimeOfOplog != 0) {
            // 设置查询数据的时间范围
            condition.append("ts", new Document().append("$gte", docTime));
        }
        if (endTimeOfOplog != 0) {
            // 带有范围的oplog
            condition.append("ts", new Document().append("$gte", docTime).append("&lte", new BsonTimestamp(endTimeOfOplog, 0)));
        }
        Log.info("程序:" + proName + ",读取oplog.rs的条件为:" + condition);
        try {
            MongoCollection oplogCollection = mongoClient.getDatabase("local").getCollection("oplog.rs");
            MongoCursor<Document> cursor =
                    oplogCollection.find(condition).projection(projectionField).
                            sort(new Document("$natural", 1)).
                            cursorType(CursorType.TailableAwait).noCursorTimeout(true).batchSize(8192).iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                String ns = document.get("ns").toString();
                // 记录当前oplog的时间
                oplogMetadata.lastOplogTs = (BsonTimestamp) document.get("ts");
                {
                    // 进行判断此程序的状态
                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.REAL_TIME_RUN) {
                        // 此线程需要停止了
                        if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                            cursor.close();
                        } else if (proStatus == ProStatus.REAL_TIME_SLEEP) {
                            // 此线程需要进行睡眠
                            final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                            synchronized (proRealTimeObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,oplog读取线程即将睡眠");
                                proRealTimeObjectLock.wait();
                            }
                        }
                    }
                }
                // 心累 单独为3.2进行判断建立索引
                if (programInfo.getSourceVersion().startsWith("3") && ns.endsWith(".system.indexes")) {
                    // 可能为建立索引
                    Document o = (Document) document.get("o");
                    // 插入数据 但是没有_id 。则是建立索引的oplog
                    if (!o.containsKey("_id")) {
                        // 开始构造通用的建立索引的oplog
                        document.put("op", "c");
                        String dbName = ns.split("\\.", 2)[0];
                        document.put("ns", dbName + ".$cmd");
                        String tableName = o.get("ns").toString().split("\\.", 2)[1];
                        o.put("createIndexes", tableName);
                        o.remove("ns");
                        document.put("o", o);
                        ns = dbName + ".$cmd";
                    }
                }
                // 库表名过滤.
                if (!ns.matches(dbTableWhite)) {
                    // 没有通过库表过滤,则进入判断是否通过ddl判断
                    // cmd的进行下一级步骤进行过滤
                    if (!filterDdl || !document.get("op").equals("c")) {
                        continue;
                    }
                }

                // 过滤元数据库信息 local admin config库数据不同步
                if (!ns.startsWith("local.") && !ns.startsWith("admin.") && !ns.startsWith("config.")) {
                    // 保留本次oplog的ts读取时间
                    docTime = (BsonTimestamp) document.get("ts");
                    // 当前时间减去oplog的时间减去  小于延迟时间即可
                    if ((System.currentTimeMillis() / 1000) - docTime.getTime() < delayTime) {
                        TimeUnit.MINUTES.sleep(1);
                    }
                    if (readNum++ > 1000000) {
                        readNum = 0;
                        oplogMetadata.millionNum++;
                    }
                    oplogMetadata.documentQueueOfOplog.put(document);
                }
            }
            // 如果程序能够正常走到这里 则代表查询完毕 更新程序的状态
            ProStatus.updateProStatus(proName, ProStatus.REAL_TIME_STOP);
            isReadScanOver = true;
        } catch (Exception e) {
            isReadScanOver = false;
            // 重新更新查询的开始时间和结束时间
            this.startTimeOfOplog = docTime.getTime();
            this.inc = docTime.getInc();
            Log.error("程序:" + proName + ",读取oplog发现异常,错误信息为:" + e.getMessage());
        }
    }
}
