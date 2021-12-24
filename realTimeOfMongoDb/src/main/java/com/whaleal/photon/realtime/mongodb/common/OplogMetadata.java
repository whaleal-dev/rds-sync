package com.whaleal.photon.realtime.mongodb.common;


import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractPhotonObject;
import lombok.*;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@Getter
@Setter
@ToString
/**
 * @desc: oplog的元数据类。每个source只有一个OplogMetadata
 * @author: lhp
 * @time: 2021/7/30 10:46 上午
 */
public final class OplogMetadata extends AbstractPhotonObject {
    /**
     * 源数据源名称 程序名+'source'
     */
    public String sourceDsName;
    /**
     * 目标数据源名称 程序名+'target'
     */
    public String targetDsName;
    /**
     * 表过滤策略 使用正则表达式进行过滤
     */
    public String dbTableWhite;
    /**
     * 要同步的DDL列表
     */
    public Set<String> ddlList;
    /**
     * 每个数据源最多存储的oplog数据量。缓存未处理的oplog的大小
     */
    public int maxDocumentQueueSizeOfOplog;
    /**
     * 每个ns最多存储的数据量
     */
    private int maxTableQueueSizeOfNs;
    /**
     * 每个桶最多存储的批次数
     */
    private int maxTableBatchNumOfBucket;
    /**
     * 每批数据存储的个数
     */
    private int maxBatchNum;
    /**
     * 桶个数
     */
    private int maxBucketNum;
    /**
     * 批次号
     */
    public long batchNo;
    /**
     * 百万次次数
     */
    public long millionNum = 0L;

    public BsonTimestamp lastOplogTs = new BsonTimestamp();

    public Map<String, LongAdder> bulkWriteInfo = new ConcurrentHashMap<>();

    {
        bulkWriteInfo.put("insert", new LongAdder());
        bulkWriteInfo.put("delete", new LongAdder());
        bulkWriteInfo.put("update", new LongAdder());
        bulkWriteInfo.put("cmd", new LongAdder());
    }

    public Long getBulkWriteInfo() {
        long sum = bulkWriteInfo.get("insert").sum() +
                bulkWriteInfo.get("delete").sum() +
                bulkWriteInfo.get("update").sum() +
                bulkWriteInfo.get("cmd").sum();
        return sum;
    }

    public OplogMetadata(ProgramInfo programInfo) {
        super(programInfo.getTaskName(), programInfo.getProName());
        this.batchNo = programInfo.getBatchNo();
        this.sourceDsName = programInfo.getSourceDsName();
        this.targetDsName = programInfo.getTargetDsName();
        this.dbTableWhite = programInfo.getDbTableWhite();
        // 暂时这个几个参数 可以写死。性能尚可
        this.maxBucketNum = 20;
        this.maxDocumentQueueSizeOfOplog = 102400;
        this.maxTableQueueSizeOfNs = 8096;
        this.maxTableBatchNumOfBucket = 40;
        this.ddlList = new HashSet<>();
        this.documentQueueOfOplog = new LinkedBlockingQueue<Document>(this.maxDocumentQueueSizeOfOplog);
    }

    /**
     * 原始oplogDocument数据
     */
    public BlockingQueue<Document> documentQueueOfOplog;
    /**
     * 保存每个表的document
     * k为表名，v为ns解析后的Document
     */
    public final Map<String, BlockingQueue<Document>> dbTableQueueOfNsMap = new ConcurrentHashMap<>();
    /**
     * k桶号
     * v为批数据
     */
    public final Map<Integer, BlockingQueue<BatchDataEntityOfMongodb>> dbTableBucketBatchDataQueueMap = new ConcurrentHashMap<>();
    /**
     * k为表名
     * v为原子类AtomicBoolean
     * 用来判断某表进行nsBucket时，是否被占用
     */
    public final Map<String, AtomicBoolean> dbTableIsUseOfNsBucket = new ConcurrentHashMap<>();
    /**
     * 桶名
     * v为原子类AtomicBoolean
     * 用来判断某桶，是否被占用
     */
    public final Map<Integer, AtomicBoolean> dbTableBucketIsUseOfWrite = new ConcurrentHashMap<>();
    /**
     * 桶名
     * v为原子类AtomicBoolean
     * 用来判断某桶被写完一桶数据/或写完桶数据的次数
     */
    public final Map<Integer, AtomicInteger> dbTableBucketIsEmptyCount = new ConcurrentHashMap<>();
    /**
     * 每个表创建/删除的时间
     * k为库表名称
     * v为drop_时间戳，create_时间戳
     */
    public final Map<String, String> dbTableCreateOrDropTimeMap = new ConcurrentHashMap<>();
    /**
     * ddl存储的map集合
     * k为库表名称+索引名
     * v docment
     */
    public final Map<String, Document> dbTableIndexMap = new ConcurrentHashMap<>();
    /**
     * 删除或创建索引时，修改此参数的时间
     */
    public volatile long createOrDropIndexTime = 0L;
    /**
     * 同步对象
     */
    public final Object syncObject = new Object();
    /**
     * k为库表名称+索引名
     * v为原子类AtomicBoolean
     * 用来判断某桶被写完一桶数据/或写完桶数据的次数
     */
    public final Map<String, AtomicBoolean> dbTableIndexIsUserMap = new ConcurrentHashMap<>();


    public int dbTableQueueOfNsDataNum() {
        int sum = 0;
        Iterator<Map.Entry<String, BlockingQueue<Document>>> iterator = dbTableQueueOfNsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            sum = iterator.next().getValue().size();
        }
        return sum;
    }

    public int dbTableBucketBatchDataQueueDataNum() {
        int sum = 0;
        Iterator<Map.Entry<Integer, BlockingQueue<BatchDataEntityOfMongodb>>> iterator = dbTableBucketBatchDataQueueMap.entrySet().iterator();
        while (iterator.hasNext()) {
            sum = iterator.next().getValue().size();
        }
        return sum;
    }
}
