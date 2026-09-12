package com.whaleal.rds.sink.kafka.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 目标配置。{@code target.uri} 视为 bootstrap servers（可带 {@code kafka://} 前缀）。
 * <p>
 * 本类不依赖 {@code kafka-clients}；P1b 的 Producer 再消费这些字段。
 */
public final class KafkaSinkConfig {

    public static final String DEFAULT_TOPIC_SEPARATOR = ".";
    public static final String DEFAULT_ACKS = "all";
    public static final String DEFAULT_COMPRESSION = "lz4";
    public static final String DEFAULT_CLIENT_ID = "rds-sync";
    public static final int DEFAULT_LINGER_MS = 5;
    public static final int DEFAULT_BATCH_SIZE_BYTES = 16384;

    private String bootstrapServers;
    private String schema;
    private String table;
    /** 固定 topic；非空时忽略 prefix/schema/table 拼接。 */
    private String topic;
    private String topicPrefix = "";
    private String topicSeparator = DEFAULT_TOPIC_SEPARATOR;
    private String topicSuffix = "";
    /** 非空时 DDL 发往该 topic，否则与行事件同 topic。 */
    private String ddlTopic;
    private boolean publishDdl = true;
    private String acks = DEFAULT_ACKS;
    private int lingerMs = DEFAULT_LINGER_MS;
    private int batchSizeBytes = DEFAULT_BATCH_SIZE_BYTES;
    private String compressionType = DEFAULT_COMPRESSION;
    private String clientId = DEFAULT_CLIENT_ID;
    private Map<String, String> extraProducerProperties = Collections.emptyMap();

    private KafkaSinkConfig() {
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public String getTopic() {
        return topic;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public String getTopicSeparator() {
        return topicSeparator;
    }

    public String getTopicSuffix() {
        return topicSuffix;
    }

    public String getDdlTopic() {
        return ddlTopic;
    }

    public boolean isPublishDdl() {
        return publishDdl;
    }

    public String getAcks() {
        return acks;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public int getBatchSizeBytes() {
        return batchSizeBytes;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public String getClientId() {
        return clientId;
    }

    public Map<String, String> getExtraProducerProperties() {
        return extraProducerProperties;
    }

    /**
     * 去掉 {@code kafka://} / {@code kafka:} 前缀，得到 bootstrap.servers。
     */
    public static String normalizeBootstrap(String uri) {
        if (uri == null) {
            return null;
        }
        String s = uri.trim();
        if (s.regionMatches(true, 0, "kafka://", 0, 8)) {
            return s.substring(8).trim();
        }
        if (s.regionMatches(true, 0, "kafka:", 0, 6)) {
            return s.substring(6).trim();
        }
        return s;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final KafkaSinkConfig c = new KafkaSinkConfig();

        public Builder bootstrapServers(String bootstrapServers) {
            c.bootstrapServers = normalizeBootstrap(bootstrapServers);
            return this;
        }

        public Builder schema(String schema) {
            c.schema = schema;
            return this;
        }

        public Builder table(String table) {
            c.table = table;
            return this;
        }

        public Builder topic(String topic) {
            c.topic = topic;
            return this;
        }

        public Builder topicPrefix(String topicPrefix) {
            c.topicPrefix = topicPrefix == null ? "" : topicPrefix;
            return this;
        }

        public Builder topicSeparator(String topicSeparator) {
            c.topicSeparator = (topicSeparator == null || topicSeparator.isEmpty())
                    ? DEFAULT_TOPIC_SEPARATOR : topicSeparator;
            return this;
        }

        public Builder topicSuffix(String topicSuffix) {
            c.topicSuffix = topicSuffix == null ? "" : topicSuffix;
            return this;
        }

        public Builder ddlTopic(String ddlTopic) {
            c.ddlTopic = ddlTopic;
            return this;
        }

        public Builder publishDdl(boolean publishDdl) {
            c.publishDdl = publishDdl;
            return this;
        }

        public Builder acks(String acks) {
            c.acks = (acks == null || acks.trim().isEmpty()) ? DEFAULT_ACKS : acks.trim();
            return this;
        }

        public Builder lingerMs(int lingerMs) {
            c.lingerMs = lingerMs < 0 ? DEFAULT_LINGER_MS : lingerMs;
            return this;
        }

        public Builder batchSizeBytes(int batchSizeBytes) {
            c.batchSizeBytes = batchSizeBytes > 0 ? batchSizeBytes : DEFAULT_BATCH_SIZE_BYTES;
            return this;
        }

        public Builder compressionType(String compressionType) {
            c.compressionType = (compressionType == null || compressionType.trim().isEmpty())
                    ? DEFAULT_COMPRESSION : compressionType.trim();
            return this;
        }

        public Builder clientId(String clientId) {
            c.clientId = (clientId == null || clientId.trim().isEmpty())
                    ? DEFAULT_CLIENT_ID : clientId.trim();
            return this;
        }

        public Builder extraProducerProperties(Map<String, String> extraProducerProperties) {
            if (extraProducerProperties == null || extraProducerProperties.isEmpty()) {
                c.extraProducerProperties = Collections.emptyMap();
            } else {
                c.extraProducerProperties = Collections.unmodifiableMap(
                        new LinkedHashMap<String, String>(extraProducerProperties));
            }
            return this;
        }

        public KafkaSinkConfig build() {
            return c;
        }
    }
}
