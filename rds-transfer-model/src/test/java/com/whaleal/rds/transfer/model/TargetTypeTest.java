package com.whaleal.rds.transfer.model;

import org.junit.Assert;
import org.junit.Test;

public class TargetTypeTest {

    @Test
    public void parseDefaultsToMysql() {
        Assert.assertEquals(TargetType.MYSQL, TargetType.parse(null));
        Assert.assertEquals(TargetType.MYSQL, TargetType.parse(""));
        Assert.assertEquals(TargetType.MYSQL, TargetType.parse(" mysql "));
        Assert.assertEquals(TargetType.MYSQL, TargetType.parse("jdbc"));
        Assert.assertEquals(TargetType.MYSQL, TargetType.parse("RDS"));
    }

    @Test
    public void parseKafka() {
        Assert.assertEquals(TargetType.KAFKA, TargetType.parse("kafka"));
        Assert.assertTrue(TargetType.KAFKA.isKafka());
        Assert.assertFalse(TargetType.KAFKA.isMysql());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsUnknown() {
        TargetType.parse("mongodb");
    }
}
