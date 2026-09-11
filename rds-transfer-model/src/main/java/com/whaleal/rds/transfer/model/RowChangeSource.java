package com.whaleal.rds.transfer.model;

/**
 * 捕获位点摘要（binlog / SCN / LSN 等，具体格式由 Source 解释）。
 */
public final class RowChangeSource {

    private final String connector;
    private final String snapshot;
    private final String offset;

    public RowChangeSource(String connector, String snapshot, String offset) {
        this.connector = connector;
        this.snapshot = snapshot;
        this.offset = offset;
    }

    public String getConnector() {
        return connector;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public String getOffset() {
        return offset;
    }
}
