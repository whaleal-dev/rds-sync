package com.whaleal.rds.sync.sdk;

/**
 * 进度快照骨架（字段随 P1 补齐）。
 */
public final class MigrationProgress {

    private final MigrationState state;
    private final boolean canCommit;
    private final boolean fullSyncComplete;
    private final boolean incrementalPaused;
    private final String detail;

    public MigrationProgress(MigrationState state,
                             boolean canCommit,
                             boolean fullSyncComplete,
                             boolean incrementalPaused,
                             String detail) {
        this.state = state;
        this.canCommit = canCommit;
        this.fullSyncComplete = fullSyncComplete;
        this.incrementalPaused = incrementalPaused;
        this.detail = detail;
    }

    public MigrationState getState() {
        return state;
    }

    public boolean isCanCommit() {
        return canCommit;
    }

    public boolean isFullSyncComplete() {
        return fullSyncComplete;
    }

    public boolean isIncrementalPaused() {
        return incrementalPaused;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "MigrationProgress{state=" + state
                + ", canCommit=" + canCommit
                + ", fullSyncComplete=" + fullSyncComplete
                + ", incrementalPaused=" + incrementalPaused
                + ", detail='" + detail + "'}";
    }
}
