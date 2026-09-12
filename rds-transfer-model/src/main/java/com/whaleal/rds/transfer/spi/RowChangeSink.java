package com.whaleal.rds.transfer.spi;

import com.whaleal.rds.transfer.model.DdlEvent;
import com.whaleal.rds.transfer.model.RowChange;

/**
 * 目标端写入 SPI：只识别 {@link RowChange} / {@link DdlEvent}，不关心上游是 JDBC 快照还是 CDC。
 * <p>
 * MYSQL（JDBC）与 KAFKA 各自实现；Pipeline / Client 只依赖本契约。
 * 对齐 mongo-sync {@code TransferSink}。
 */
public interface RowChangeSink extends AutoCloseable {

    /**
     * 写入一条行事件。
     *
     * @return 本次写入序号；{@code 0} 表示未产生写入
     */
    long write(RowChange event);

    /**
     * 已确认落地的最大写入序号：所有 {@code seq <= landedThrough()} 的写入都已在目标端生效
     * （JDBC 落库或 Kafka produce ack）。
     */
    long landedThrough();

    /** 先排空在途 CRUD，再落地 DDL（JDBC 执行 SQL；Kafka 按配置投递消息）。 */
    void applyDdl(DdlEvent event);

    /** JDBC 可调整有序写；Kafka 实现可忽略。 */
    void setOrdered(boolean ordered);

    /** 刷写缓冲并等待在途完成（不关闭资源）。 */
    void flushAndWait();

    @Override
    void close();
}
