package task;


import cache.MemoryCache;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import common.taskbase.SourceTaskInfo;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.dataclass.Range;
import common.taskbase.SourceTaskInterface;
import conf.Configuration;

import execute.MongodbSource;
import dbconnection.mongodb.MongoDbConnection;
import org.bson.Document;
import parse.TransformationMongodbDataToColumn;
import thread.SourceTaskPoolManager;
import util.Log;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取表某区间数据
 */
public class MongodbSourceTask implements Runnable, SourceTaskInterface {

    private MemoryCache memoryCache;

    private String procName;
    /**
     * 任务配置信息
     */
    private SourceTaskInfo taskMetadata;
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 缓存大小
     */
    private long cache = 0L;
    /**
     * 每个批次数据的大小
     */
    public int dataBatchSize = 128;
    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();


    public MongodbSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        this.procName = procName;
        this.memoryCache = memoryCache;
        this.dataBatchSize = dataBatchSize;
        this.taskMetadata = taskMetadata;
        this.mongoClient = MongoDbConnection.getMongoClient(this.taskMetadata.getSourceDsName());
    }


    @Override
    public void run() {

        Log.info("启动source任务:" + this.taskMetadata.toString());
        // 读取数据
        getDataFromCollection();
        SourceTaskPoolManager.setSourceActiveThreadNum(procName, -1);
    }

    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    @Override
    public void getDataFromCollection() {
        String[] strings = this.taskMetadata.getDbTableName().split("\\.", 2);
        String dbName = strings[0];
        String tableName = strings[1];
        Range range = this.taskMetadata.getRange();
        Object minId = range.getMinId();
        Object maxId = range.getMaxId();
        Object minIdTemp = minId;
        BasicDBObject condition = new BasicDBObject();
        condition.append("_id", new Document("$lt", maxId).append("$gte", minId));
        // 如果是range最大范围，则查询范围是[]。否则[)
        if (range.isMax()) {
            condition.append("_id", new Document("$lte", maxId).append("$gte", minId));
        }
        // 设置range的开始时间
        range.setStartTime(System.currentTimeMillis());
        try {
            //读取collection中的数据
            MongoCursor<Document> mongoCursor = this.mongoClient.getDatabase(dbName).getCollection(tableName).
                    find(condition).sort(new BasicDBObject().append("_id", 1)).iterator();
            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                dataTransformation(document);
                if (cache++ > dataBatchSize) {
                    putDataToCache();
                }
                if (document != null && document.get("_id") != null) {
                    minIdTemp = document.get("_id");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
            Range rangeTem = new Range();
            rangeTem.setMinId(minIdTemp);
            rangeTem.setMaxId(maxId);
            rangeTem.setMax(range.isMax());
            // 出现意外时，再次启动该任务实例
            SourceTaskInfo taskMetadata = new SourceTaskInfo(rangeTem, this.taskMetadata.getDbTableName(), this.taskMetadata.getSourceDsName());
            MongodbSource.pushTaskMeta(procName, taskMetadata);
        } finally {
            // 设置range的结束时间。设置range的开始结束时间，后期会使用到该参数
            range.setEndTime(System.currentTimeMillis());
            // 推送最后一批数据
            if (cache > 0) {
                putDataToCache();
                this.dataList = null;
            }
            Log.info("source任务查询完毕:" + this.taskMetadata.toString());
        }
    }

    public void dataTransformation(Object document) {
        List<AbstractColumn> abstractColumns = new ArrayList<>();
        Iterator<Map.Entry<String, Object>> iterator = ((Document) document).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            AbstractColumn abstractColumn = TransformationMongodbDataToColumn.parseValue(next.getKey(), next.getValue());
            abstractColumns.add(abstractColumn);
        }
        this.dataList.add(abstractColumns);
    }

    static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * putDataToCache 推送数据到缓存区中
     *
     * @desc 推送数据到缓存区中
     */
    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        batchDataEntity.setDataList(this.dataList);
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName().split("\\.")[0] + "bak." + this.taskMetadata.getDbTableName().split("\\.")[1]);
        batchDataEntity.setOperation("INSERTMANY");
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        System.out.println("sourceNum:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
