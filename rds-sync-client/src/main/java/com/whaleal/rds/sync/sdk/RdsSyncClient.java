package com.whaleal.rds.sync.sdk;

/**
 * 编排客户端骨架。P1 起接入 Source / Pipeline / Sink；此处仅固定控制面 API。
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
