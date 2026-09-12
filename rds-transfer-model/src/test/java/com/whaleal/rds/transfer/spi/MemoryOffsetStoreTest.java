package com.whaleal.rds.transfer.spi;

import org.junit.Assert;
import org.junit.Test;

public class MemoryOffsetStoreTest {

    @Test
    public void saveLoadAndClear() {
        MemoryOffsetStore store = new MemoryOffsetStore();
        Assert.assertNull(store.load("demo.orders"));
        store.save("demo.orders", "mysql-bin.000001:4");
        Assert.assertEquals("mysql-bin.000001:4", store.load("demo.orders"));
        store.save("demo.orders", null);
        Assert.assertNull(store.load("demo.orders"));
    }
}
