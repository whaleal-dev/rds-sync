package com.whaleal.rds.sync.sink;

import com.whaleal.rds.sync.config.RdsSyncConfig;
import com.whaleal.rds.transfer.model.SinkType;
import com.whaleal.rds.transfer.spi.RowChangeSink;

/**
 * 按 {@link SinkType} 创建 {@link RowChangeSink}。
 * <p>
 * Pipeline / Client 只依赖 {@link RowChangeSink}。MYSQL 实现见 P1（{@code rds-mysql-sink}），
 * KAFKA 实现见 P1b（{@code rds-kafka-sink}）。此处先锁定装配点，避免后续把 JDBC / Kafka 写进 Pipeline。
 */
public final class SinkFactory {

    private SinkFactory() {
    }

    public static RowChangeSink create(RdsSyncConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config is required");
        }
        if (config.getSinkType() == SinkType.KAFKA) {
            throw new UnsupportedOperationException(
                    "Kafka RowChangeSink is P1b; topic/envelope contract is in rds-kafka-sink");
        }
        throw new UnsupportedOperationException(
                "MYSQL RowChangeSink is P1; rds-mysql-sink must implement RowChangeSink");
    }
}
