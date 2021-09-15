package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.common.util.Log;

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
    public String targetDsName;
    /**
     * oplog元数据库类
     */
    private OplogMetadata oplogMetadata;
    /**
     * mongoClient
     */
    private MongoClient mongoClient;


    public OplogWriteTask(OplogMetadata oplogMetadata) {
        this.oplogMetadata = oplogMetadata;
        this.targetDsName = oplogMetadata.targetDsName;
        this.mongoClient = MongoDbConnection.getMongoClient(oplogMetadata.procNameAndBatchNo + targetDsName);
        System.out.println("OplogWriteTask");
    }

    @Override
    public void run() {
        int IdlingTimes = 0;
        while (true) {
            try {
                if (IdlingTimes++ > 20) {
                    TimeUnit.SECONDS.sleep(1);
                    //Log.info(oplogMetadata.sourceDsName + ",OplogWriteTaskWait");
                    IdlingTimes = 19;
                }
                Iterator<Map.Entry<String, BlockingQueue<BatchDataEntity>>> iterator = oplogMetadata.dbTableBucketBatchDataQueueMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    try {
                        Map.Entry<String, BlockingQueue<BatchDataEntity>> next = iterator.next();
                        // 表名+桶号
                        String dbTableBucketNum = next.getKey();
                        boolean pre = oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).get();
                        if (!pre && oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).compareAndSet(false, true)) {
                            // 解析后的WriteModel队列
                            Queue<BatchDataEntity> documentQueue = next.getValue();
                            if (documentQueue.size() == 0) {
                                IdlingTimes++;
                                oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).set(false);
                                continue;
                            }
                            //Log.info("当前解析的dbTableBucketNum:" + dbTableBucketNum);
                            IdlingTimes = 0;
                            // 写入该表的数据
                            write(documentQueue);
                            oplogMetadata.dbTableBucketIsUseOfWrite.get(dbTableBucketNum).set(false);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.error(e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
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
        int parseSize = 0;
        while (true) {
            BatchDataEntity batchDataEntity = documentQueue.poll();
            // batchDataEntity为null或已经写入了20批数据，则退出
            if (batchDataEntity == null) {
                break;
            }
            bulkExecute(batchDataEntity);
            if (parseSize++ > 10) {
                break;
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
            //Log.info("当前批次执行情况:"+bulkWriteResult.toString());
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
                //  Log.error(e.getMessage());
            }
        }
    }

}
