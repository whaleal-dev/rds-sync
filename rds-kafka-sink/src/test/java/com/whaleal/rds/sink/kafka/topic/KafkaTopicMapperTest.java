package com.whaleal.rds.sink.kafka.topic;

import com.whaleal.rds.sink.kafka.config.KafkaSinkConfig;
import org.junit.Assert;
import org.junit.Test;

public class KafkaTopicMapperTest {

    @Test
    public void defaultIsSchemaSeparatorTable() {
        KafkaTopicMapper mapper = new KafkaTopicMapper(base().build());
        Assert.assertEquals("demo.orders", mapper.topic("demo", "orders"));
    }

    @Test
    public void prefixAndSuffixMatchMongoSyncShape() {
        KafkaTopicMapper mapper = new KafkaTopicMapper(base()
                .topicPrefix("rds")
                .topicSuffix("cdc")
                .build());
        Assert.assertEquals("rds.demo.orders.cdc", mapper.topic("demo", "orders"));
    }

    @Test
    public void fixedTopicWins() {
        KafkaTopicMapper mapper = new KafkaTopicMapper(base()
                .topic("all-changes")
                .topicPrefix("rds")
                .build());
        Assert.assertEquals("all-changes", mapper.topic("demo", "orders"));
        Assert.assertTrue(mapper.isFixedTopic());
    }

    @Test
    public void ddlTopicOverride() {
        KafkaTopicMapper mapper = new KafkaTopicMapper(base()
                .ddlTopic("schema-changes")
                .build());
        Assert.assertEquals("demo.orders", mapper.topic("demo", "orders"));
        Assert.assertEquals("schema-changes", mapper.ddlTopic("demo", "orders"));
    }

    @Test
    public void ddlFallsBackToRowTopic() {
        KafkaTopicMapper mapper = new KafkaTopicMapper(base().topicPrefix("rds").build());
        Assert.assertEquals("rds.demo.orders", mapper.ddlTopic("demo", "orders"));
    }

    private static KafkaSinkConfig.Builder base() {
        return KafkaSinkConfig.builder().bootstrapServers("localhost:9092");
    }
}
