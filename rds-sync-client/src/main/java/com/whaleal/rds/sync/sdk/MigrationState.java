package com.whaleal.rds.sync.sdk;

/**
 * 迁移状态（对齐 mongo-sync MigrationState）。
 */
public enum MigrationState {
    IDLE,
    RUNNING,
    PAUSED,
    CAN_COMMIT,
    COMMITTING,
    COMMITTED,
    STOPPED,
    ERROR
}
