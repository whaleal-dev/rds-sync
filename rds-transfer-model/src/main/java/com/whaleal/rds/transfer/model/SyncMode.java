package com.whaleal.rds.transfer.model;

/**
 * 同步模式（对齐 mongo-sync SyncMode 语义）。
 */
public enum SyncMode {
    FULL,
    INCREMENTAL,
    FULL_AND_INCREMENTAL,
    FULL_AND_CATCH_UP;

    public boolean includesFull() {
        return this == FULL || this == FULL_AND_INCREMENTAL || this == FULL_AND_CATCH_UP;
    }

    public boolean includesIncremental() {
        return this == INCREMENTAL || this == FULL_AND_INCREMENTAL || this == FULL_AND_CATCH_UP;
    }

    public boolean isCatchUpThenStop() {
        return this == FULL_AND_CATCH_UP;
    }

    public static SyncMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        return SyncMode.valueOf(value.trim().toUpperCase());
    }
}
