package com.whaleal.rds.transfer.model;

/**
 * 同步目标形态（对齐 mongo-sync {@code TargetType}）。
 */
public enum TargetType {

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

    public static TargetType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MYSQL;
        }
        String v = value.trim().toUpperCase();
        if ("JDBC".equals(v) || "MYSQL".equals(v) || "RDS".equals(v)) {
            return MYSQL;
        }
        return TargetType.valueOf(v);
    }
}
