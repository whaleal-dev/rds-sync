package com.whaleal.rds.transfer.model;

/**
 * 同步模式（对齐 mongo-sync SyncMode 语义）。
 * <p>
 * {@code AND} 表示全量与增量并行并持续；{@code THEN} 表示先全量、再追平、再停。
 */
public enum SyncMode {
    FULL,
    INCREMENTAL,
    FULL_AND_INCREMENTAL,
    FULL_THEN_CATCH_UP;

    public boolean includesFull() {
        return this == FULL || this == FULL_AND_INCREMENTAL || this == FULL_THEN_CATCH_UP;
    }

    public boolean includesIncremental() {
        return this == INCREMENTAL || this == FULL_AND_INCREMENTAL || this == FULL_THEN_CATCH_UP;
    }

    public boolean isCatchUpThenStop() {
        return this == FULL_THEN_CATCH_UP;
    }

    /** 全量与增量是否并行。仅 {@link #FULL_AND_INCREMENTAL}。 */
    public boolean parallelFullAndIncremental() {
        return this == FULL_AND_INCREMENTAL;
    }

    public static SyncMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FULL;
        }
        return SyncMode.valueOf(value.trim().toUpperCase());
    }
}
