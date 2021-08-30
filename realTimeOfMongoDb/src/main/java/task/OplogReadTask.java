package task;

import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import common.OplogMetadata;
import conf.Configuration;
import dbconnection.mongodb.MongoDbConnection;
import org.bson.BsonTimestamp;
import org.bson.Document;
import thread.SourceTaskPoolManager;
import util.Log;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取oplog中的数据
 */
public class OplogReadTask implements Runnable {
    private Configuration configuration;

    private String procName;
    /**
     * 数据源名称
     */
    private String sourceDsName;
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 表过滤策略
     */
    private String dbTableWhite;
    /**
     * 开始读取该数据源的时间
     */
    private int startTimeOfReady;
    /**
     * 开始读取该数据源的毫秒
     */
    private int inc;
    /**
     * 结束读取该数据源的时间
     */
    private int endTimeOfReady;
    /**
     * 是否同步DDL
     */
    private boolean filterDdl;
    /**
     * oplog元数据库类
     */
    private OplogMetadata oplogMetadata;

    public OplogReadTask(Configuration configuration, int startTimeOfReady, int inc, int endTimeOfReady, OplogMetadata oplogMetadata) {
        this.configuration = configuration;
        this.filterDdl = configuration.isFilterDdl();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.sourceDsName = configuration.getSourceDsName();
        this.endTimeOfReady = endTimeOfReady;
        this.startTimeOfReady = startTimeOfReady;
        this.inc = inc;
        this.mongoClient = MongoDbConnection.getMongoClient(this.sourceDsName);
        this.oplogMetadata = oplogMetadata;
    }

    @Override
    public void run() {
        try {
            BsonTimestamp startTime = new BsonTimestamp(startTimeOfReady, inc);
            // 获取oplog的最早时间
            BsonTimestamp oplogStartTime = getStartOfOplog();
            Log.info("检查是否错过滑动窗口时间:{sourceDsName:" + sourceDsName + ",startTime:" + startTime + ",oplogStartTime" + oplogStartTime + "}");
            // 如果oplog的开始时间小于startTimeOfReady，即全表同步期间oplog没有被覆盖
            // startTimeOfReady=0时代表为增量抽取数据。抽取范围[minTs,正无穷)
            if (startTime.compareTo(oplogStartTime) > 0 || startTimeOfReady == 0) {
                Log.info("sourceDsName:" + sourceDsName + ",开始同步oplog的数据");
                source();
            } else {
                Log.info("sourceDsName:" + sourceDsName + ",错过滑动窗口时间,读取oplog失败,oplog被覆盖");
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
            Log.error(e.getMessage());
        }
    }

    /**
     * getStartOfOplog 获取oplog的开始时间
     *
     * @desc 获取oplog的第一条数据的时间
     */
    public BsonTimestamp getStartOfOplog() {
        MongoCollection topicCollection = mongoClient.getDatabase("local").getCollection("oplog.rs");
        Document document = (Document) topicCollection.find().first();
        BsonTimestamp bsonTimestamp = (BsonTimestamp) document.get("ts");
        return bsonTimestamp;
    }

    /**
     * source 读取oplog的中数据
     *
     * @desc 读取oplog的中数据
     */
    public void source() {
        BsonTimestamp docTime = new BsonTimestamp(startTimeOfReady, inc);
        Log.info("sourceDsName:" + sourceDsName + "{time:" + docTime + ",开始读取oplog的数据}");
        BasicDBObject condition = new BasicDBObject();
        if (startTimeOfReady != 0) {
            // 设置查询数据的时间范围
            condition.append("ts", new Document().append("$gte", docTime));
        }
        try {
            MongoCollection oplogCollection = mongoClient.getDatabase("local").getCollection("oplog.rs");
            MongoCursor<Document> cursor =
                    oplogCollection.find(condition).
                            sort(new Document("$natural", 1)).
                            cursorType(CursorType.TailableAwait).noCursorTimeout(true).batchSize(8192).iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                String ns = document.get("ns").toString();
                // 库表名过滤.
                if (!ns.matches(dbTableWhite)) {
                    //没有通过库表过滤，则进入判断是否通过ddl判断
                    if (!filterDdl || !ns.contains(".$cmd")) {
                        continue;
                    }
                }
                // 过滤元数据库信息
                if (!ns.startsWith("local.") && !ns.startsWith("admin.") && !ns.startsWith("config.")) {
                    if (document != null && document.get("ts") != null) {
                        docTime = (BsonTimestamp) document.get("ts");
                    }
                    //Log.info(document.toJson());
                    // 添加数据到documentQueue
                    oplogMetadata.documentQueue.put(document);
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            Log.error("sourceDsName:" + sourceDsName + ",读取oplog发现异常:" + e.getMessage());
        } finally {
            SourceTaskPoolManager.submit(procName, new OplogReadTask(configuration, docTime.getTime(), docTime.getInc(), endTimeOfReady, oplogMetadata));
        }
    }
}
