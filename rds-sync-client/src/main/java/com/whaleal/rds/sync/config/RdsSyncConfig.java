package com.whaleal.rds.sync.config;

import com.whaleal.rds.sink.kafka.config.KafkaSinkConfig;
import com.whaleal.rds.transfer.model.SyncMode;
import com.whaleal.rds.transfer.model.TargetType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 同步任务配置骨架。P1 起补 Source / Pipeline 字段；此处先锁定双目标形态。
 */
public final class RdsSyncConfig {

    private SyncMode syncMode = SyncMode.FULL;
    /** 源端 JDBC URL。 */
    private String sourceUri;
    private TargetType targetType = TargetType.MYSQL;
    /** MYSQL：JDBC URL；KAFKA：bootstrap servers（允许 {@code kafka://} 前缀）。 */
    private String targetUri;
    /** MYSQL 可预建表；KAFKA 强制为 false。 */
    private boolean bootstrapTable = true;

    private String kafkaTopic;
    private String kafkaTopicPrefix = "";
    private String kafkaTopicSeparator = KafkaSinkConfig.DEFAULT_TOPIC_SEPARATOR;
    private String kafkaTopicSuffix = "";
    private String kafkaDdlTopic;
    private boolean kafkaPublishDdl = true;
    private String kafkaAcks = KafkaSinkConfig.DEFAULT_ACKS;
    private int kafkaLingerMs = KafkaSinkConfig.DEFAULT_LINGER_MS;
    private int kafkaBatchSizeBytes = KafkaSinkConfig.DEFAULT_BATCH_SIZE_BYTES;
    private String kafkaCompressionType = KafkaSinkConfig.DEFAULT_COMPRESSION;
    private String kafkaClientId = KafkaSinkConfig.DEFAULT_CLIENT_ID;
    private Map<String, String> kafkaProducerProperties = Collections.emptyMap();

    private RdsSyncConfig() {
    }

    public SyncMode getSyncMode() {
        return syncMode == null ? SyncMode.FULL : syncMode;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public TargetType getTargetType() {
        return targetType == null ? TargetType.MYSQL : targetType;
    }

    public String getTargetUri() {
        return targetUri;
    }

    public boolean isBootstrapTable() {
        return bootstrapTable;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public String getKafkaTopicPrefix() {
        return kafkaTopicPrefix;
    }

    public String getKafkaTopicSeparator() {
        return kafkaTopicSeparator;
    }

    public String getKafkaTopicSuffix() {
        return kafkaTopicSuffix;
    }

    public String getKafkaDdlTopic() {
        return kafkaDdlTopic;
    }

    public boolean isKafkaPublishDdl() {
        return kafkaPublishDdl;
    }

    public String getKafkaAcks() {
        return kafkaAcks;
    }

    public int getKafkaLingerMs() {
        return kafkaLingerMs;
    }

    public int getKafkaBatchSizeBytes() {
        return kafkaBatchSizeBytes;
    }

    public String getKafkaCompressionType() {
        return kafkaCompressionType;
    }

    public String getKafkaClientId() {
        return kafkaClientId;
    }

    public Map<String, String> getKafkaProducerProperties() {
        return kafkaProducerProperties;
    }

    /** 供 P1b 装配 {@code rds-kafka-sink}。 */
    public KafkaSinkConfig toKafkaSinkConfig() {
        if (getTargetType() != TargetType.KAFKA) {
            throw new IllegalStateException("toKafkaSinkConfig requires targetType=KAFKA");
        }
        return KafkaSinkConfig.builder()
                .bootstrapServers(targetUri)
                .topic(kafkaTopic)
                .topicPrefix(kafkaTopicPrefix)
                .topicSeparator(kafkaTopicSeparator)
                .topicSuffix(kafkaTopicSuffix)
                .ddlTopic(kafkaDdlTopic)
                .publishDdl(kafkaPublishDdl)
                .acks(kafkaAcks)
                .lingerMs(kafkaLingerMs)
                .batchSizeBytes(kafkaBatchSizeBytes)
                .compressionType(kafkaCompressionType)
                .clientId(kafkaClientId)
                .extraProducerProperties(kafkaProducerProperties)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final RdsSyncConfig c = new RdsSyncConfig();

        public Builder syncMode(SyncMode syncMode) {
            c.syncMode = syncMode == null ? SyncMode.FULL : syncMode;
            return this;
        }

        public Builder sourceUri(String sourceUri) {
            c.sourceUri = sourceUri;
            return this;
        }

        public Builder targetType(TargetType targetType) {
            c.targetType = targetType == null ? TargetType.MYSQL : targetType;
            return this;
        }

        public Builder targetUri(String targetUri) {
            c.targetUri = targetUri;
            return this;
        }

        public Builder bootstrapTable(boolean bootstrapTable) {
            c.bootstrapTable = bootstrapTable;
            return this;
        }

        public Builder kafkaTopic(String kafkaTopic) {
            c.kafkaTopic = kafkaTopic;
            return this;
        }

        public Builder kafkaTopicPrefix(String kafkaTopicPrefix) {
            c.kafkaTopicPrefix = kafkaTopicPrefix == null ? "" : kafkaTopicPrefix;
            return this;
        }

        public Builder kafkaTopicSeparator(String kafkaTopicSeparator) {
            c.kafkaTopicSeparator = (kafkaTopicSeparator == null || kafkaTopicSeparator.isEmpty())
                    ? KafkaSinkConfig.DEFAULT_TOPIC_SEPARATOR : kafkaTopicSeparator;
            return this;
        }

        public Builder kafkaTopicSuffix(String kafkaTopicSuffix) {
            c.kafkaTopicSuffix = kafkaTopicSuffix == null ? "" : kafkaTopicSuffix;
            return this;
        }

        public Builder kafkaDdlTopic(String kafkaDdlTopic) {
            c.kafkaDdlTopic = kafkaDdlTopic;
            return this;
        }

        public Builder kafkaPublishDdl(boolean kafkaPublishDdl) {
            c.kafkaPublishDdl = kafkaPublishDdl;
            return this;
        }

        public Builder kafkaAcks(String kafkaAcks) {
            c.kafkaAcks = kafkaAcks;
            return this;
        }

        public Builder kafkaLingerMs(int kafkaLingerMs) {
            c.kafkaLingerMs = kafkaLingerMs;
            return this;
        }

        public Builder kafkaBatchSizeBytes(int kafkaBatchSizeBytes) {
            c.kafkaBatchSizeBytes = kafkaBatchSizeBytes;
            return this;
        }

        public Builder kafkaCompressionType(String kafkaCompressionType) {
            c.kafkaCompressionType = kafkaCompressionType;
            return this;
        }

        public Builder kafkaClientId(String kafkaClientId) {
            c.kafkaClientId = kafkaClientId;
            return this;
        }

        public Builder kafkaProducerProperties(Map<String, String> kafkaProducerProperties) {
            if (kafkaProducerProperties == null || kafkaProducerProperties.isEmpty()) {
                c.kafkaProducerProperties = Collections.emptyMap();
            } else {
                c.kafkaProducerProperties = Collections.unmodifiableMap(
                        new LinkedHashMap<String, String>(kafkaProducerProperties));
            }
            return this;
        }

        public RdsSyncConfig build() {
            if (c.sourceUri == null || c.sourceUri.trim().isEmpty()) {
                throw new IllegalArgumentException("sourceUri is required");
            }
            TargetType type = c.getTargetType();
            if (c.targetUri == null || c.targetUri.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        type == TargetType.KAFKA
                                ? "targetUri (Kafka bootstrap servers) is required when targetType=KAFKA"
                                : "targetUri (JDBC URL) is required when targetType=MYSQL");
            }
            if (type == TargetType.KAFKA) {
                c.targetUri = KafkaSinkConfig.normalizeBootstrap(c.targetUri);
                if (c.targetUri == null || c.targetUri.isEmpty()) {
                    throw new IllegalArgumentException(
                            "targetUri (Kafka bootstrap servers) is required when targetType=KAFKA");
                }
                c.bootstrapTable = false;
            }
            return c;
        }
    }
}
