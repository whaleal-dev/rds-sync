package com.whaleal.rds.transfer.spi;

/**
 * 有序分桶管道：消费 Source 事件，只向 {@link RowChangeSink} 写出。
 * <p>
 * 禁止依赖 JDBC Driver 或 {@code kafka-clients}。MYSQL / KAFKA 由 Client 按
 * {@code target.type} 注入 Sink。
 */
public interface RowChangePipeline extends RowChangeListener, DdlEventListener, AutoCloseable {

    void attach(RowChangeSink sink);

    /** 等待在途 CRUD 落地（DDL barrier / canCommit 排空）。 */
    void waitDrained();

    /** 已提交尚未 {@link RowChangeSink#landedThrough()} 的条数。 */
    long inflight();

    @Override
    void close();
}
