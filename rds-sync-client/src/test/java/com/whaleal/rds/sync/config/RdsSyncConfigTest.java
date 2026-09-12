package com.whaleal.rds.sync.config;

import com.whaleal.rds.sync.sink.TargetSinkFactory;
import com.whaleal.rds.transfer.model.SyncMode;
import com.whaleal.rds.transfer.model.TargetType;
import org.junit.Assert;
import org.junit.Test;

public class RdsSyncConfigTest {

    @Test
    public void mysqlDefaultRequiresJdbc() {
        RdsSyncConfig cfg = RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .targetUri("jdbc:mysql://127.0.0.1:3306/dst")
                .build();
        Assert.assertEquals(TargetType.MYSQL, cfg.getTargetType());
        Assert.assertEquals(SyncMode.FULL, cfg.getSyncMode());
        Assert.assertTrue(cfg.isBootstrapTable());
    }

    @Test
    public void kafkaNormalizesBootstrapAndDisablesBootstrapTable() {
        RdsSyncConfig cfg = RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .targetType(TargetType.KAFKA)
                .targetUri("kafka://127.0.0.1:9092")
                .syncMode(SyncMode.FULL_AND_INCREMENTAL)
                .kafkaTopicPrefix("rds")
                .bootstrapTable(true)
                .build();
        Assert.assertEquals("127.0.0.1:9092", cfg.getTargetUri());
        Assert.assertFalse(cfg.isBootstrapTable());
        Assert.assertEquals("rds", cfg.toKafkaSinkConfig().getTopicPrefix());
    }

    @Test(expected = IllegalStateException.class)
    public void toKafkaSinkConfigRejectedForMysql() {
        RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .targetUri("jdbc:mysql://127.0.0.1:3306/dst")
                .build()
                .toKafkaSinkConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void kafkaRequiresTargetUri() {
        RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .targetType(TargetType.KAFKA)
                .build();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void factoryLocksAssemblyPoint() {
        TargetSinkFactory.create(RdsSyncConfig.builder()
                .sourceUri("jdbc:mysql://127.0.0.1:3306/src")
                .targetType(TargetType.KAFKA)
                .targetUri("127.0.0.1:9092")
                .build());
    }
}
