package com.whaleal.rds.sync.sdk;

/**
 * 编排客户端骨架。按 {@code target.type} 装配 {@code RowChangeSink}，
 * Pipeline 只认 SPI，不感知 JDBC / Kafka。P1 起接入 Source / Pipeline。
 */
public interface RdsSyncClient extends AutoCloseable {

    void start();

    void resume();

    MigrationProgress pause();

    MigrationProgress pauseIncremental();

    MigrationProgress resumeIncremental();

    MigrationProgress progress();

    boolean canCommit();

    MigrationProgress commit();

    void stop();
}
