package com.whaleal.photon.target.mongodb.task;

import com.whaleal.photon.common.cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.common.taskbase.AbstractTargetTask;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;

import com.whaleal.photon.core.thread.TargetTaskPoolManager;
import org.bson.Document;
import com.whaleal.photon.target.mongodb.parse.ColumnDataToMongodbData;
import com.whaleal.photon.common.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */
public class MongodbTargetTask extends AbstractTargetTask {
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * 待写入的数据
     */
    private List<WriteModel<Document>> writeModels = new ArrayList<>();

    public MongodbTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.mongoClient = MongoDbConnection.getMongoClient(proName);
    }

    @Override
    public void run() {
        try {
            applyData();
        } catch (Exception e) {
            Log.error("程序:" + proName + ",写入数据时发生错误,错误信息:" + e.getMessage());
        } finally {
            TargetTaskPoolManager.setTargetActiveThreadNum(proName, -1);
        }
    }

    @Override
    public void applyData() {

        Log.info("程序:" + proName + ",启动target任务");
        int writesNumber = 0;
        long writesCount = 0L;

        while (true) {
            try {

                BatchDataEntity batchDataEntity = memoryCache.getData();
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的dbTableName
                    this.dbTableName = batchDataEntity.getDbTableName();
                    parseColumnDataToTargetData(batchDataEntity);
                    bulkExecute(dbTableName, -1);
                    writesCount += writeModels.size();
                    // 避免频繁的进行writeDocCount.add对性能造成损失
                    if (writesNumber++ > 100) {
                        // 更新写入的总条数
                        memoryCache.writeDocCount.add(writesCount);
                        writesNumber = 0;
                        writesCount = 0L;
                    }
                    bulkExecute(dbTableName,-1);
                } else {
                    memoryCache.writeDocCount.add(writesCount);
                    writesNumber = 0;
                    writesCount = 0L;
//                    // 可以进行睡眠
                    TimeUnit.SECONDS.sleep(1);
                    // 既然睡眠了检查一下缓存区情况
                    if (memoryCache.getAllDataBucketNum() == 0) {
                        Integer proStatus = ProStatus.getProStatus(proName);
                        if (proStatus != ProStatus.FULL_SYNC_RUN) {
                            // 此线程需要停止了
                            if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.FULL_SYNC_STOP) {
                                Log.warn("程序:" + proName + ",检测到该全量程序进入STOP状态,target线程即将关闭");
                                break;
                            } else if (proStatus == ProStatus.FULL_SYNC_SLEEP_AND_REAL_TIME_RUN || proStatus == ProStatus.FULL_SYNC_SLEEP) {
                                // 此线程需要进行睡眠
                                final Object proFullSyncObjectLock = ProStatus.getProFullSyncObjectLock(proName);
                                synchronized (proFullSyncObjectLock) {
                                    Log.warn("程序:" + proName + ",检测到该全量程序进入SLEEP状态,target线程即将睡眠");
                                    proFullSyncObjectLock.wait();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",写入数据时发生错误,错误信息:" + e.getMessage());
            }
        }
    }

    @Override
    public void parseColumnDataToTargetData(BatchDataEntity batchDataEntity) {
        //可以细分回滚字段范围。以防单个字段进行数据删除时，由于数据量大，造成任务过长时间卡顿
        int partition = (int) ((Math.random() * 100) % 10);
        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            Document document = new Document();
            for (AbstractColumn columnData : columnList) {
                document.append(columnData.getColumnName(), ColumnDataToMongodbData.parseColumnData(columnData));
            }
           // document.append("procNameAndBatchNo", proNameAndBatchNo + "_" + partition);
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
