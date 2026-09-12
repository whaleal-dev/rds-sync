package com.whaleal.rds.sink.kafka.config;

import org.junit.Assert;
import org.junit.Test;

public class KafkaSinkConfigTest {

    @Test
    public void normalizeStripsKafkaScheme() {
        Assert.assertEquals("127.0.0.1:9092", KafkaSinkConfig.normalizeBootstrap("kafka://127.0.0.1:9092"));
        Assert.assertEquals("127.0.0.1:9092", KafkaSinkConfig.normalizeBootstrap("kafka:127.0.0.1:9092"));
        Assert.assertEquals("127.0.0.1:9092", KafkaSinkConfig.normalizeBootstrap("127.0.0.1:9092"));
    }

    @Test
    public void builderStoresNormalizedBootstrap() {
        KafkaSinkConfig cfg = KafkaSinkConfig.builder()
                .bootstrapServers("kafka://broker:9092")
                .topicPrefix("rds")
                .build();
        Assert.assertEquals("broker:9092", cfg.getBootstrapServers());
        Assert.assertEquals("rds", cfg.getTopicPrefix());
        Assert.assertTrue(cfg.isPublishDdl());
    }
}
