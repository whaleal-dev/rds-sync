package common;

import common.dataclass.BatchDataEntity;
import conf.Configuration;
import lombok.*;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
/**
 * @desc: oplog的元数据类。每个source只有一个OplogMetadata
 * @author: lhp
 * @time: 2021/7/30 10:46 上午
 */
public final class OplogMetadata {
    private String procName;
    /**
     * 源数据源名称
     */
    public String sourceDsName;
    /**
     * 目标数据源名称
     */
    public String targetDsName;
    /**
     * 表过滤策略
     */
    public String dbTableWhite;
    /**
     * 是否同步DDL
     */
    public boolean filterDdl;
    /**
     * 每个数据源最多存储的oplog数据量
     */
    public int maxDocumentQueueSize;
    /**
     * 每个表最多存储的数据量
     */
    public int maxTableQueueSize;
    /**
     * 每个表桶最多存储的批次数
     */
    public int maxTableBatchNum;

    public OplogMetadata(Configuration configuration) {
        this.sourceDsName = configuration.getSourceDsName();
        this.targetDsName = configuration.getTargetDsName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.filterDdl = configuration.isFilterDdl();
        this.maxDocumentQueueSize = configuration.getCacheSize() * configuration.getCacheNum() * configuration.getDataBatchSize();
        this.maxTableQueueSize = configuration.getDataBatchSize();
        this.maxTableBatchNum = configuration.getCacheNum();
        this.procName = configuration.getProName();
        this.documentQueue = new LinkedBlockingQueue<Document>(maxDocumentQueueSize);
    }

    /**
     * 原始oplogDocument数据
     */
    public BlockingQueue<Document> documentQueue;
    /**
     * 保存每个表的document
     * k为表名，v为ns解析后的Document
     */
    public Map<String, BlockingQueue<Document>> dbTableQueueMap = new ConcurrentHashMap<>();
    /**
     * 保存每个表桶的document
     * k为表名，v为nsBucket解析后的Document
     */
    public Map<String, List<BatchDataEntity>> dbTableBucketListMap = new ConcurrentHashMap<>();

    /**
     * k表名+桶号
     * v为WriteModel
     */
    public Map<String, BlockingQueue<BatchDataEntity>> dbTableBucketBatchDataQueueMap = new ConcurrentHashMap<>();

    /**
     * k为表名
     * v为原子类AtomicBoolean
     */
    public Map<String, AtomicBoolean> dbTableIsUseOfNsBucket = new ConcurrentHashMap<>();
    /**
     * k为表名+桶名
     * v为原子类AtomicBoolean
     */
    public Map<String, AtomicBoolean> dbTableBucketIsUseOfWrite = new ConcurrentHashMap<>();
}
