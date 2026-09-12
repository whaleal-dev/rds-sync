package com.whaleal.rds.sync.config;

import com.whaleal.rds.sink.kafka.config.KafkaSinkConfig;
import com.whaleal.rds.transfer.model.SyncMode;
import com.whaleal.rds.transfer.model.SinkType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 同步任务配置骨架。P1 起补 Source / Pipeline 字段；此处先锁定 Source / Sink 双形态。
 */
public final class RdsSyncConfig {

    private SyncMode syncMode = SyncMode.FULL;
    /** 源端 JDBC URL。 */
    private String sourceUri;
    private SinkType sinkType = SinkType.MYSQL;
    /** MYSQL：JDBC URL；KAFKA：bootstrap servers（允许 {@code kafka://} 前缀）。 */
    private String sinkUri;
    /** MYSQL 可预建表；KAFKA 强制为 false。 */
    private boolean bootstrapTable = true;
    private String sourceUser;
    private String sourcePassword;
    private String sinkUser;
    private String sinkPassword;
    /** 库表过滤，匹配 {@code schema.table}，默认全部。 */
    private String tableWhite = ".*";
    /** Sink schema；空则与源 schema 相同。 */
    private String sinkSchema;
    private int bucketNum = 4;
    private int batchSize = 200;
    private int splitSlices = 4;

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

    public SinkType getSinkType() {
        return sinkType == null ? SinkType.MYSQL : sinkType;
    }

    public String getSinkUri() {
        return sinkUri;
    }

    public boolean isBootstrapTable() {
        return bootstrapTable;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public String getSourcePassword() {
        return sourcePassword;
    }

    public String getSinkUser() {
        return sinkUser;
    }

    public String getSinkPassword() {
        return sinkPassword;
    }

    public String getTableWhite() {
        return tableWhite == null || tableWhite.isEmpty() ? ".*" : tableWhite;
    }

    public String getSinkSchema() {
        return sinkSchema;
    }

    public int getBucketNum() {
        return bucketNum <= 0 ? 4 : bucketNum;
    }

    public int getBatchSize() {
        return batchSize <= 0 ? 200 : batchSize;
    }

    public int getSplitSlices() {
        return splitSlices <= 0 ? 4 : splitSlices;
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
        if (getSinkType() != SinkType.KAFKA) {
            throw new IllegalStateException("toKafkaSinkConfig requires sinkType=KAFKA");
        }
        return KafkaSinkConfig.builder()
                .bootstrapServers(sinkUri)
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

        public Builder sinkType(SinkType sinkType) {
            c.sinkType = sinkType == null ? SinkType.MYSQL : sinkType;
            return this;
        }

        public Builder sinkUri(String sinkUri) {
            c.sinkUri = sinkUri;
            return this;
        }

        public Builder bootstrapTable(boolean bootstrapTable) {
            c.bootstrapTable = bootstrapTable;
            return this;
        }

        public Builder sourceUser(String sourceUser) {
            c.sourceUser = sourceUser;
            return this;
        }

        public Builder sourcePassword(String sourcePassword) {
            c.sourcePassword = sourcePassword;
            return this;
        }

        public Builder sinkUser(String sinkUser) {
            c.sinkUser = sinkUser;
            return this;
        }

        public Builder sinkPassword(String sinkPassword) {
            c.sinkPassword = sinkPassword;
            return this;
        }

        public Builder tableWhite(String tableWhite) {
            c.tableWhite = tableWhite;
            return this;
        }

        public Builder sinkSchema(String sinkSchema) {
            c.sinkSchema = sinkSchema;
            return this;
        }

        public Builder bucketNum(int bucketNum) {
            c.bucketNum = bucketNum;
            return this;
        }

        public Builder batchSize(int batchSize) {
            c.batchSize = batchSize;
            return this;
        }

        public Builder splitSlices(int splitSlices) {
            c.splitSlices = splitSlices;
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
            SinkType type = c.getSinkType();
            if (c.sinkUri == null || c.sinkUri.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        type == SinkType.KAFKA
                                ? "sinkUri (Kafka bootstrap servers) is required when sinkType=KAFKA"
                                : "sinkUri (JDBC URL) is required when sinkType=MYSQL");
            }
            if (type == SinkType.KAFKA) {
                c.sinkUri = KafkaSinkConfig.normalizeBootstrap(c.sinkUri);
                if (c.sinkUri == null || c.sinkUri.isEmpty()) {
                    throw new IllegalArgumentException(
                            "sinkUri (Kafka bootstrap servers) is required when sinkType=KAFKA");
                }
                c.bootstrapTable = false;
            }
            return c;
        }
    }
}
