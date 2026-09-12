package com.whaleal.rds.sink.kafka.topic;

import com.whaleal.rds.sink.kafka.config.KafkaSinkConfig;

/**
 * Topic 命名对齐 mongo-sync Kafka Sink，仅把 {@code db.coll} 换成 {@code schema.table}：
 * {@code [prefix + sep] + schema + [sep + table] + [sep + suffix]}。
 * 配置了固定 {@code kafka.topic} 时全部事件走该 topic。
 */
public final class KafkaTopicMapper {

    private final String fixedTopic;
    private final String ddlTopic;
    private final String prefix;
    private final String separator;
    private final String suffix;

    public KafkaTopicMapper(KafkaSinkConfig config) {
        this.fixedTopic = trimToNull(config.getTopic());
        this.ddlTopic = trimToNull(config.getDdlTopic());
        this.separator = config.getTopicSeparator() == null
                ? KafkaSinkConfig.DEFAULT_TOPIC_SEPARATOR : config.getTopicSeparator();
        String rawPrefix = config.getTopicPrefix() == null ? "" : config.getTopicPrefix();
        String rawSuffix = config.getTopicSuffix() == null ? "" : config.getTopicSuffix();
        this.prefix = rawPrefix.isEmpty() ? "" : rawPrefix + separator;
        this.suffix = rawSuffix.isEmpty() ? "" : separator + rawSuffix;
    }

    public String topic(String schema, String table) {
        if (fixedTopic != null) {
            return fixedTopic;
        }
        String db = schema == null ? "" : schema;
        String tbl = table == null ? "" : table;
        String undecorated = tbl.isEmpty() ? db : db + separator + tbl;
        return prefix + undecorated + suffix;
    }

    /**
     * DDL 所用 topic：优先 {@code kafka.ddl.topic}，否则与行事件相同规则。
     */
    public String ddlTopic(String schema, String table) {
        if (ddlTopic != null) {
            return ddlTopic;
        }
        return topic(schema, table);
    }

    public boolean isFixedTopic() {
        return fixedTopic != null;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
