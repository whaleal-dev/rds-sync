package com.whaleal.rds.sink.kafka.codec;

import com.whaleal.rds.transfer.model.DdlEvent;
import com.whaleal.rds.transfer.model.RowChange;
import com.whaleal.rds.transfer.model.RowChangeSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka 行级 envelope（与 mongo-kafka Change Stream 区分）。
 * <p>
 * value 字段：{@code op} / {@code schema} / {@code table} / {@code ts_ms} /
 * {@code source} / {@code before} / {@code after}；DDL 另含 {@code sql} / {@code ddl_type}。
 * P1b 再序列化为 JSON 字节。
 */
public final class RowChangeEnvelope {

    public static final String OP = "op";
    public static final String SCHEMA = "schema";
    public static final String TABLE = "table";
    public static final String TS_MS = "ts_ms";
    public static final String SOURCE = "source";
    public static final String BEFORE = "before";
    public static final String AFTER = "after";
    public static final String SQL = "sql";
    public static final String DDL_TYPE = "ddl_type";

    public static final String OP_CREATE = "c";
    public static final String OP_UPDATE = "u";
    public static final String OP_DELETE = "d";
    public static final String OP_READ = "r";
    public static final String OP_DDL = "ddl";

    private RowChangeEnvelope() {
    }

    /** 主键列 → 值；无主键则空对象。 */
    public static Map<String, Object> key(RowChange event) {
        if (event == null) {
            return Collections.emptyMap();
        }
        List<String> pks = event.getPkColumns();
        if (pks == null || pks.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> key = new LinkedHashMap<String, Object>();
        Map<String, Object> after = event.getAfter();
        Map<String, Object> before = event.getBefore();
        for (String pk : pks) {
            Object v = lookup(after, pk);
            if (v == null) {
                v = lookup(before, pk);
            }
            key.put(pk, v);
        }
        return key;
    }

    public static Map<String, Object> value(RowChange event) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put(OP, event.getOp());
        body.put(SCHEMA, event.getSchema());
        body.put(TABLE, event.getTable());
        body.put(TS_MS, event.getTsMs());
        body.put(SOURCE, sourceMap(event.getSource(), event.getSchema(), event.getTable()));
        body.put(BEFORE, event.getBefore());
        body.put(AFTER, event.getAfter());
        return body;
    }

    public static Map<String, Object> value(DdlEvent event) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put(OP, OP_DDL);
        body.put(SCHEMA, event.getSchema());
        body.put(TABLE, event.getTable());
        body.put(TS_MS, event.getTsMs());
        body.put(DDL_TYPE, event.getType() == null ? null : event.getType().name());
        body.put(SQL, event.getSql());
        return body;
    }

    private static Map<String, Object> sourceMap(RowChangeSource source, String schema, String table) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(SCHEMA, schema);
        m.put(TABLE, table);
        if (source != null) {
            m.put("connector", source.getConnector());
            m.put("snapshot", source.getSnapshot());
            m.put("offset", source.getOffset());
        }
        return m;
    }

    private static Object lookup(Map<String, Object> cols, String pk) {
        if (cols == null || pk == null) {
            return null;
        }
        return cols.get(pk);
    }
}
