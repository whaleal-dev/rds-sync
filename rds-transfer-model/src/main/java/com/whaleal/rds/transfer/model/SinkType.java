package com.whaleal.rds.transfer.model;

/**
 * 同步 Sink 形态。mongo-sync 对位概念同样是 {@code SinkType}；两边统一用 Source / Sink。
 */
public enum SinkType {

    /** JDBC 写入 MySQL（默认）。 */
    MYSQL,

    /**
     * 写入 Kafka。消息为行级 envelope（{@code op}/{@code before}/{@code after}），
     * 与 mongo-sync 的 mongo-kafka Change Stream 格式区分。
     */
    KAFKA;

    public boolean isKafka() {
        return this == KAFKA;
    }

    public boolean isMysql() {
        return this == MYSQL;
    }

    public static SinkType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MYSQL;
        }
        String v = value.trim().toUpperCase();
        if ("JDBC".equals(v) || "MYSQL".equals(v) || "RDS".equals(v)) {
            return MYSQL;
        }
        return SinkType.valueOf(v);
    }
}
