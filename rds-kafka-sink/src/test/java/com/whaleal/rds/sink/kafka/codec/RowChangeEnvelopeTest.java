package com.whaleal.rds.sink.kafka.codec;

import com.whaleal.rds.transfer.model.DdlEvent;
import com.whaleal.rds.transfer.model.RowChange;
import com.whaleal.rds.transfer.model.RowChangeSource;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class RowChangeEnvelopeTest {

    @Test
    public void keyUsesPrimaryColumnsFromAfterThenBefore() {
        RowChange event = RowChange.builder()
                .op("u")
                .schema("demo")
                .table("orders")
                .pkColumns(Arrays.asList("id"))
                .before(Collections.<String, Object>singletonMap("id", 1))
                .after(Collections.<String, Object>singletonMap("id", 1))
                .build();
        Map<String, Object> key = RowChangeEnvelope.key(event);
        Assert.assertEquals(1, key.get("id"));
        Assert.assertEquals(1, key.size());
    }

    @Test
    public void keyEmptyWithoutPk() {
        RowChange event = RowChange.builder()
                .op("c")
                .schema("demo")
                .table("orders")
                .after(Collections.<String, Object>singletonMap("name", "a"))
                .build();
        Assert.assertTrue(RowChangeEnvelope.key(event).isEmpty());
    }

    @Test
    public void valueCarriesRowEnvelopeFields() {
        RowChange event = RowChange.builder()
                .op("c")
                .schema("demo")
                .table("orders")
                .tsMs(100L)
                .source(new RowChangeSource("mysql", "true", "file:1"))
                .after(Collections.<String, Object>singletonMap("id", 9))
                .build();
        Map<String, Object> value = RowChangeEnvelope.value(event);
        Assert.assertEquals("c", value.get(RowChangeEnvelope.OP));
        Assert.assertEquals("demo", value.get(RowChangeEnvelope.SCHEMA));
        Assert.assertEquals("orders", value.get(RowChangeEnvelope.TABLE));
        Assert.assertEquals(Long.valueOf(100L), value.get(RowChangeEnvelope.TS_MS));
        Assert.assertNull(value.get(RowChangeEnvelope.BEFORE));
        Assert.assertEquals(9, ((Map<?, ?>) value.get(RowChangeEnvelope.AFTER)).get("id"));
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) value.get(RowChangeEnvelope.SOURCE);
        Assert.assertEquals("mysql", source.get("connector"));
        Assert.assertEquals("file:1", source.get("offset"));
    }

    @Test
    public void ddlValueUsesDdlOpAndSql() {
        DdlEvent ddl = DdlEvent.builder()
                .type(DdlEvent.Type.ALTER_TABLE)
                .schema("demo")
                .table("orders")
                .sql("alter table orders add col int")
                .tsMs(2L)
                .build();
        Map<String, Object> value = RowChangeEnvelope.value(ddl);
        Assert.assertEquals(RowChangeEnvelope.OP_DDL, value.get(RowChangeEnvelope.OP));
        Assert.assertEquals("ALTER_TABLE", value.get(RowChangeEnvelope.DDL_TYPE));
        Assert.assertEquals("alter table orders add col int", value.get(RowChangeEnvelope.SQL));
        Assert.assertFalse(value.containsKey(RowChangeEnvelope.AFTER));
    }
}
