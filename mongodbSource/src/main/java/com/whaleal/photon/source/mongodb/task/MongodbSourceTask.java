package com.whaleal.photon.source.mongodb.task;


import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.whaleal.photon.common.common.operation.OperationFlag;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceTask;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.dataclass.Range;

import com.whaleal.photon.source.mongodb.exexute.MongodbSourceExecute;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import org.bson.Document;
import com.whaleal.photon.source.mongodb.parse.MongodbDataToColumnData;
import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.*;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取表某区间数据
 */
public class MongodbSourceTask extends AbstractSourceTask {
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();

    public MongodbSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        this.mongoClient = MongoDbConnection.getMongoClient(getProcNameAndBatchNoAndSourceDsName());
    }

    @Override
    public void run() {
        try {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 1);
            Log.info("启动source任务:" + this.taskMetadata.toString());
            // 读取数据
            getDataFromDbTable();
        } finally {
            // source线程数-1
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), -1);
            Log.info("source任务查询完毕:" + this.taskMetadata.toString());
        }
    }


    @Override
    public void dataTransformation(Object document) {
        List<AbstractColumn> abstractColumns = new ArrayList<>();
        Iterator<Map.Entry<String, Object>> iterator = ((Document) document).entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            AbstractColumn abstractColumn = MongodbDataToColumnData.parseValue(next.getKey(), next.getValue(), isUserDeFaultType);
            abstractColumns.add(abstractColumn);
        }
        this.dataList.add(abstractColumns);
    }

    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */

    @Override
    public void getDataFromDbTable() {
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
            // 重新生产一个任务实例
            MongodbSourceExecute.pushTaskMeta(getProcNameAndBatchNo(), taskMetadata);
        } finally {
            // 设置range的结束时间。设置range的开始结束时间，后期会使用到该参数
            range.setEndTime(System.currentTimeMillis());
            // 推送最后一批数据
            if (cache > 0) {
                putDataToCache();
                this.dataList = null;
            }
        }
    }

    /**
     * putDataToCache 推送数据到缓存区中
     *
     * @desc 推送数据到缓存区中
     */
    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        batchDataEntity.setDataList(this.dataList);
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName());
        batchDataEntity.setOperation(OperationFlag.INSERTMANY);
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
