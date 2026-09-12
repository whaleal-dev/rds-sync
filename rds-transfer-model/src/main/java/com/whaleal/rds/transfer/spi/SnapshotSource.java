package com.whaleal.rds.transfer.spi;

/**
 * 全量快照源：只产出 {@link com.whaleal.rds.transfer.model.RowChange}（{@code op=r}），
 * 不感知 MYSQL / KAFKA Sink。
 * <p>
 * PhotonT 的 {@code *SourceExecute} 经适配器实现本接口（P1 / P3），不得把
 * {@code BatchDataEntity} / {@code MemoryCache} 泄漏到 Pipeline。
 */
public interface SnapshotSource extends AutoCloseable {

    void start(RowChangeListener listener);

    boolean isComplete();

    void stop();

    @Override
    void close();
}
