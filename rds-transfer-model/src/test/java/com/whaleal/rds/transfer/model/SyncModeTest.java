package com.whaleal.rds.transfer.model;

import org.junit.Assert;
import org.junit.Test;

public class SyncModeTest {

    @Test
    public void parseAndFlags() {
        Assert.assertEquals(SyncMode.FULL, SyncMode.parse(null));
        Assert.assertEquals(SyncMode.FULL_AND_INCREMENTAL, SyncMode.parse("full_and_incremental"));
        Assert.assertTrue(SyncMode.FULL.includesFull());
        Assert.assertFalse(SyncMode.FULL.includesIncremental());
        Assert.assertTrue(SyncMode.FULL_THEN_CATCH_UP.isCatchUpThenStop());
        Assert.assertFalse(SyncMode.FULL_THEN_CATCH_UP.parallelFullAndIncremental());
        Assert.assertTrue(SyncMode.FULL_AND_INCREMENTAL.parallelFullAndIncremental());
        Assert.assertFalse(SyncMode.FULL_AND_INCREMENTAL.isCatchUpThenStop());
    }
}
