package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;

import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.thread.RealTimeTaskPoolManager;
import com.whaleal.photon.realtime.mongodb.common.BatchDataEntityOfMongodb;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.Document;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: lhp
 * @time: 2021/7/30 11:56 上午
 */
public class OplogWriteTask implements Runnable {
    /**
     * 目标数据源
     */
    public final String targetDsName;
    /**
     * oplog元数据库类
     */
    private final OplogMetadata oplogMetadata;
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * 程序名
     */
    private final String proName;


    public OplogWriteTask(OplogMetadata oplogMetadata, String proName) {
        this.oplogMetadata = oplogMetadata;
        this.targetDsName = oplogMetadata.targetDsName;
        this.mongoClient = MongoDbConnection.getMongoClient(this.targetDsName);
        this.proName = proName;
    }

    @Override
    public void run() {
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_WRITE, 1);
        int IdlingTimes = 0;
        while (true) {
            try {
                if (IdlingTimes++ > 10) {
                    TimeUnit.SECONDS.sleep(1);
                    IdlingTimes = 8;
                    // 既然空闲了 那就把检查一下dbTableBucketIsEmptyCount的值是否超过一亿
                    int buckNumTemp = (int) ((Math.random() * 1000) % oplogMetadata.getMaxBucketNum());
                    int count = oplogMetadata.dbTableBucketIsEmptyCount.get(buckNumTemp).getAndIncrement();
                    // 一亿
                    if (count > 100000000) {
                        oplogMetadata.dbTableBucketIsEmptyCount.get(buckNumTemp).set(0);
                    }
                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.REAL_TIME_RUN) {
                        // 此线程需要停止了
                        if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                            if (oplogMetadata.dbTableQueueOfNsDataNum() == 0 && oplogMetadata.documentQueueOfOplog.size() == 0 && oplogMetadata.dbTableBucketBatchDataQueueDataNum() == 0) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入STOP状态,oplog写入线程即将停止");
                                break;
                            }
                        } else if (proStatus == ProStatus.FULL_SYNC_RUN_AND_REAL_TIME_SLEEP || proStatus == ProStatus.REAL_TIME_SLEEP) {
                            if (oplogMetadata.dbTableQueueOfNsDataNum() != 0 || oplogMetadata.documentQueueOfOplog.size() != 0 || oplogMetadata.dbTableBucketBatchDataQueueDataNum() != 0) {
                                continue;
                            }
                            // 此线程需要进行睡眠
                            final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                            synchronized (proRealTimeObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,oplog写入线程即将睡眠");
                                proRealTimeObjectLock.wait();
                            }
                        }
                    }
                }
                Iterator<Map.Entry<Integer, BlockingQueue<BatchDataEntityOfMongodb>>> iterator = oplogMetadata.dbTableBucketBatchDataQueueMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Integer, BlockingQueue<BatchDataEntityOfMongodb>> next = iterator.next();
                    // 桶号
                    Integer bucketNum = next.getKey();
                    boolean pre = oplogMetadata.dbTableBucketIsUseOfWrite.get(bucketNum).get();
                    if (!pre && oplogMetadata.dbTableBucketIsUseOfWrite.get(bucketNum).compareAndSet(false, true)) {
                        // 解析后的WriteModel队列
                        Queue<BatchDataEntityOfMongodb> documentQueue = next.getValue();
                        if (documentQueue.size() == 0) {
                            IdlingTimes++;
                            oplogMetadata.dbTableBucketIsUseOfWrite.get(bucketNum).set(false);
                            oplogMetadata.dbTableBucketIsEmptyCount.get(bucketNum).incrementAndGet();
                            continue;
                        }
                        IdlingTimes = 0;
                        // 写入该表的数据
                        write(documentQueue, bucketNum);
                        oplogMetadata.dbTableBucketIsUseOfWrite.get(bucketNum).set(false);
                        oplogMetadata.dbTableBucketIsEmptyCount.get(bucketNum).incrementAndGet();
                    }
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",oplog写入时发生错误,错误信息:" + e.getMessage());
            }
        }
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_WRITE, -1);
    }

    /**
     * write
     *
     * @param documentQueue
     * @desc 执行写入
     */
    public void write(Queue<BatchDataEntityOfMongodb> documentQueue, int bucketNum) {
        int parseSize = 0;
        while (true) {
            BatchDataEntityOfMongodb batchDataEntity = documentQueue.poll();
            // batchDataEntity为null或已经写入了20批数据，则退出
            if (batchDataEntity == null) {
                break;
            }
            bulkExecute(batchDataEntity);
            if (parseSize++ > oplogMetadata.getMaxTableBatchNumOfBucket()) {
                break;
            }
        }
    }

    /**
     * bulkExecute 批量写数据
     *
     * @desc 批量写数据
     */
    public void bulkExecute(BatchDataEntityOfMongodb batchDataEntity) {
        try {
            List<WriteModel<Document>> list = batchDataEntity.getDataList();
            if (list.size() == 0) {
                return;
            }
            String dbTableName = batchDataEntity.getDbTableName();
            String dbName = dbTableName.split("\\.", 2)[0];
            String tableName = dbTableName.split("\\.", 2)[1];
            BulkWriteResult bulkWriteResult = this.mongoClient.getDatabase(dbName).getCollection(tableName).bulkWrite(list, new BulkWriteOptions().ordered(false));
            bulkWriteInfo(bulkWriteResult);
        } catch (Exception ignored) {

        }
    }

    private void bulkWriteInfo(BulkWriteResult bulkWriteResult) {
        int insertedCount = bulkWriteResult.getInsertedCount();
        int deletedCount = bulkWriteResult.getDeletedCount();
        int modifiedCount = bulkWriteResult.getModifiedCount();
        oplogMetadata.bulkWriteInfo.get("insert").add(insertedCount);
        oplogMetadata.bulkWriteInfo.get("update").add(modifiedCount);
        oplogMetadata.bulkWriteInfo.get("delete").add(deletedCount);
    }

}
