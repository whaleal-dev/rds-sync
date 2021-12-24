package com.whaleal.photon.source.mongodb.task;


import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;

import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;

import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.common.taskbase.AbstractSourceTask;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.thread.SourceTaskPoolManager;
import com.whaleal.photon.source.mongodb.parse.MongodbDataToColumnData;
import lombok.Synchronized;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取表某区间数据
 */
public class MongodbSourceTask extends AbstractSourceTask {
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();

    /**
     * 用于计算现在这批数据量的大小
     */
    private int cacheTemp = 0;

    private boolean scanOver = false;

    private int writeNum = 0;

    public MongodbSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        // source线程数+1
        SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
        this.mongoClient = MongoDbConnection.getMongoClient(programInfo.getSourceDsName());
    }


    @Override
    public void run() {
        // 任务失败后 可以继续进行向下执行
        while (!scanOver) {
            try {
                // 进行判断此程序的状态
                Integer proStatus = ProStatus.getProStatus(proName);
                if (proStatus != ProStatus.FULL_SYNC_RUN) {
                    // 此线程需要停止了
                    if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.FULL_SYNC_STOP) {
                        Log.warn("程序:" + proName + ",检测到该全量程序进入STOP状态,source线程即将关闭");
                        break;
                    } else if (proStatus == ProStatus.FULL_SYNC_SLEEP_AND_REAL_TIME_RUN || proStatus == ProStatus.FULL_SYNC_SLEEP) {
                        // 此线程需要进行睡眠
                        final Object proFullSyncObjectLock = ProStatus.getProFullSyncObjectLock(proName);
                        synchronized (proFullSyncObjectLock) {
                            Log.warn("程序:" + proName + ",检测到该全量程序进入SLEEP状态,source线程即将睡眠");
                            proFullSyncObjectLock.wait();
                        }
                    }
                }
                // 设置taskMetadata的开始时间，后期会使用到该参数
                taskMetadata.setStartTime(System.currentTimeMillis());
                Log.info("程序:" + proName + ",启动source任务:" + this.taskMetadata.toString());
                // 读取数据
                getDataFromDbTable();
            } catch (Exception ignored) {

            } finally {
                // 设置taskMetadata的结束时间，后期会使用到该参数
                taskMetadata.setEndTime(System.currentTimeMillis());
                taskMetadata.getRange().setRangeSize(writeNum);
            }
        }
        // source线程数-1
        SourceTaskPoolManager.setSourceActiveThreadNum(proName, -1);
        long timeDiff = (this.taskMetadata.getEndTime() - this.taskMetadata.getStartTime()) / 1000;
        Log.info("程序:" + proName + ",source任务查询完毕:" + this.taskMetadata.toString() + ",用时" + timeDiff + "S,读取" + writeNum + "条数据");
    }


    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    @Override
    public void getDataFromDbTable() {
        String[] split = this.taskMetadata.getDbTableName().split("\\.", 2);
        String dbName = split[0];
        String tableName = split[1];
        Range range = this.taskMetadata.getRange();
        String columnName = range.getColumnName();
        Object minValue = range.getMinValue();
        Object maxValue = range.getMaxValue();
        Object minValueTemp = minValue;
        BasicDBObject condition = new BasicDBObject();
        condition.append("_id", new Document("$lt", maxValue).append("$gte", minValue));
        // 如果是range最大范围，则查询范围是[]。否则[)
        if (range.isMax()) {
            condition.append("_id", new Document("$lte", maxValue).append("$gte", minValue));
        }
        try {
            // 读取collection中的数据
            MongoCursor<Document> mongoCursor = this.mongoClient.getDatabase(dbName).getCollection(tableName).
                    find(condition).sort(new BasicDBObject().append("_id", 1)).iterator();
            while (mongoCursor.hasNext()) {
                writeNum++;
                Document document = mongoCursor.next();
                dataTransformation(document);
                // 满一批数据时
                if (cacheTemp++ > dataBatchSize) {
                    putDataToCache();
                    //判断该读取任务是否要睡眠之类操作  注 关闭不需要在此处处理
                    // 进行判断此程序的状态
                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.FULL_SYNC_RUN) {
                        // 此线程需要停止了
                        if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.FULL_SYNC_STOP) {
                            // 推送最后一批数据
                            putDataToCache();
                            Log.warn("程序:" + proName + ",检测到该全量程序进入STOP状态,source线程即将关闭");
                            mongoCursor.close();
                        } else if (proStatus == ProStatus.FULL_SYNC_SLEEP_AND_REAL_TIME_RUN || proStatus == ProStatus.FULL_SYNC_SLEEP) {
                            putDataToCache();
                            // 此线程需要进行睡眠
                            final Object proFullSyncObjectLock = ProStatus.getProFullSyncObjectLock(proName);
                            synchronized (proFullSyncObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该全量程序进入SLEEP状态,source线程即将睡眠");
                                proFullSyncObjectLock.wait();
                            }
                        }
                    }
                }
                // 不需要进行校验是否为空之类的
                minValueTemp = document.get("_id");
            }
            scanOver = true;
        } catch (Exception e) {
            Log.error("程序:" + proName + ",读取[" + range.toString() + "]发生错误,错误信息:" + e.getMessage());
            Range rangeTemp = new Range();
            rangeTemp.setMinValue(minValueTemp);
            rangeTemp.setMaxValue(maxValue);
            rangeTemp.setMax(range.isMax());
            // 出现意外时，再次启动该任务实例
            this.taskMetadata = new SourceTaskInfo(rangeTemp, this.taskMetadata.getDbTableName(), this.taskMetadata.getSourceDsName());
            scanOver = false;
        } finally {
            // 推送最后一批数据
            if (cacheTemp > 0) {
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
        if (cacheTemp == 0) {
            return;
        }
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        batchDataEntity.setDataList(this.dataList);
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName());
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        this.dataList = new ArrayList<>();
        this.cacheTemp = 0;

    }

    @Override
    public void dataTransformation(Object document) {
        List<AbstractColumn> abstractColumns = new ArrayList<>();
        for (Map.Entry<String, Object> next : ((Document) document).entrySet()) {
            AbstractColumn abstractColumn = MongodbDataToColumnData.parseValue(next.getKey(), next.getValue(), isUserDeFaultType);
            abstractColumns.add(abstractColumn);
        }
        this.dataList.add(abstractColumns);
    }

}
