package com.whaleal.rds.sync.config;

import com.whaleal.rds.sync.sink.SinkFactory;
import com.whaleal.rds.transfer.model.SyncMode;
import com.whaleal.rds.transfer.model.SinkType;
import org.junit.Assert;
import org.junit.Test;

public class RdsSyncConfigTest {

    @Test
    public void mysqlDefaultRequiresJdbc() {
        RdsSyncConfig cfg = RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .sinkUri("jdbc:mysql://127.0.0.1:3306/dst")
                .build();
        Assert.assertEquals(SinkType.MYSQL, cfg.getSinkType());
        Assert.assertEquals(SyncMode.FULL, cfg.getSyncMode());
        Assert.assertTrue(cfg.isBootstrapTable());
    }

    @Test
    public void kafkaNormalizesBootstrapAndDisablesBootstrapTable() {
        RdsSyncConfig cfg = RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .sinkType(SinkType.KAFKA)
                .sinkUri("kafka://127.0.0.1:9092")
                .syncMode(SyncMode.FULL_AND_INCREMENTAL)
                .kafkaTopicPrefix("rds")
                .bootstrapTable(true)
                .build();
        Assert.assertEquals("127.0.0.1:9092", cfg.getSinkUri());
        Assert.assertFalse(cfg.isBootstrapTable());
        Assert.assertEquals("rds", cfg.toKafkaSinkConfig().getTopicPrefix());
    }

    @Test(expected = IllegalStateException.class)
    public void toKafkaSinkConfigRejectedForMysql() {
        RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .sinkUri("jdbc:mysql://127.0.0.1:3306/dst")
                .build()
                .toKafkaSinkConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void kafkaRequiresTargetUri() {
        RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .sinkType(SinkType.KAFKA)
                .build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void factoryLocksAssemblyPoint() {
        SinkFactory.create(RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .sinkType(SinkType.KAFKA)
                .sinkUri("127.0.0.1:9092")
                .build());
    }
}
