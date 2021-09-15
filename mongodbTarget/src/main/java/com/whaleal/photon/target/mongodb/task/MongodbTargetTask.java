package com.whaleal.photon.target.mongodb.task;

import com.whaleal.photon.common.cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.taskbase.AbstractTargetTask;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;

import org.bson.Document;
import com.whaleal.photon.target.mongodb.parse.ColumnDataToMongodbData;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */
public class MongodbTargetTask extends AbstractTargetTask {
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 待写入的数据
     */
    private List<WriteModel<Document>> writeModels = new ArrayList<>();

    public MongodbTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.mongoClient = MongoDbConnection.getMongoClient(getProcNameAndBatchNoAndTargetDsName());
    }

    @Override
    public void run() {
        try {
            applyData();
        } finally {
            TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), -1);
        }
    }

    @Override
    public void applyData() {
        String proNameAndBatchNo = getProcNameAndBatchNo();
        Log.info("启动target任务:" + proNameAndBatchNo);
        while (true) {
            try {
                if (AbstractTargetTask.getIsStopFlagOfTarget(proNameAndBatchNo)) {
                    break;
                }
                BatchDataEntity batchDataEntity = memoryCache.getData();
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的dbTableName
                    this.dbTableName = batchDataEntity.getDbTableName();
                    parseColumnDataToTargetData(batchDataEntity);
                    bulkExecute(dbTableName, -1);
                } else {
                    //可以进行睡眠
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void parseColumnDataToTargetData(BatchDataEntity batchDataEntity) {
        String proNameAndBatchNo = getProcNameAndBatchNo();
        //可以细分回滚字段范围。以防单个字段进行数据删除时，由于数据量大，造成任务过长时间卡顿
        int partition = (int) ((Math.random() * 100) % 10);
        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            Document document = new Document();
            for (AbstractColumn columnData : columnList) {
                document.append(columnData.getColumnName(), ColumnDataToMongodbData.parseColumnData(columnData));
            }
            document.append("procNameAndBatchNo", proNameAndBatchNo + "_" + partition);
            writeModels.add(new InsertOneModel<>(document));
        }
    }

    @Override
    public void bulkExecute(String dbTable, long batchNo) {
        try {
            if (writeModels.size() == 0) {
                return;
            }
            String[] dbTableArray = dbTable.split("\\.", 2);
            String dbName = dbTableArray[0];
            String tableName = dbTableArray[1];
            this.mongoClient.getDatabase(dbName).
                    getCollection(tableName).bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            writeModels = new ArrayList<>();
        }
    }
}
