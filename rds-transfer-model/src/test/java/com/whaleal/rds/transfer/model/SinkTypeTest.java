package com.whaleal.rds.transfer.model;

import org.junit.Assert;
import org.junit.Test;

public class SinkTypeTest {

    @Test
    public void parseDefaultsToMysql() {
        Assert.assertEquals(SinkType.MYSQL, SinkType.parse(null));
        Assert.assertEquals(SinkType.MYSQL, SinkType.parse(""));
        Assert.assertEquals(SinkType.MYSQL, SinkType.parse(" mysql "));
        Assert.assertEquals(SinkType.MYSQL, SinkType.parse("jdbc"));
        Assert.assertEquals(SinkType.MYSQL, SinkType.parse("RDS"));
    }

    @Test
    public void parseKafka() {
        Assert.assertEquals(SinkType.KAFKA, SinkType.parse("kafka"));
        Assert.assertTrue(SinkType.KAFKA.isKafka());
        Assert.assertFalse(SinkType.KAFKA.isMysql());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsUnknown() {
        SinkType.parse("mongodb");
    }
}
