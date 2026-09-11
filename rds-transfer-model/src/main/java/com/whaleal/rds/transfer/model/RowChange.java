package com.whaleal.rds.transfer.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 关系型行变更事件（全量 snapshot 与 CDC 共用）。
 * <p>
 * {@code op}: {@code c}/{@code u}/{@code d}/{@code r}（snapshot）。
 */
public final class RowChange {

    private final String op;
    private final String schema;
    private final String table;
    private final Map<String, Object> before;
    private final Map<String, Object> after;
    private final List<String> pkColumns;
    private final Long tsMs;
    private final RowChangeSource source;

    private RowChange(Builder b) {
        this.op = b.op;
        this.schema = b.schema;
        this.table = b.table;
        this.before = b.before == null ? null : Collections.unmodifiableMap(b.before);
        this.after = b.after == null ? null : Collections.unmodifiableMap(b.after);
        this.pkColumns = b.pkColumns == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(b.pkColumns);
        this.tsMs = b.tsMs;
        this.source = b.source;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOp() {
        return op;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public Map<String, Object> getBefore() {
        return before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

    public List<String> getPkColumns() {
        return pkColumns;
    }

    public Long getTsMs() {
        return tsMs;
    }

    public RowChangeSource getSource() {
        return source;
    }

    public String qualifiedTable() {
        if (schema == null || schema.isEmpty()) {
            return table;
        }
        return schema + "." + table;
    }

    public static final class Builder {
        private String op;
        private String schema;
        private String table;
        private Map<String, Object> before;
        private Map<String, Object> after;
        private List<String> pkColumns;
        private Long tsMs;
        private RowChangeSource source;

        public Builder op(String op) {
            this.op = op;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder before(Map<String, Object> before) {
            this.before = before;
            return this;
        }

        public Builder after(Map<String, Object> after) {
            this.after = after;
            return this;
        }

        public Builder pkColumns(List<String> pkColumns) {
            this.pkColumns = pkColumns;
            return this;
        }

        public Builder tsMs(Long tsMs) {
            this.tsMs = tsMs;
            return this;
        }

        public Builder source(RowChangeSource source) {
            this.source = source;
            return this;
        }

        public RowChange build() {
            if (op == null || op.isEmpty()) {
                throw new IllegalArgumentException("op is required");
            }
            if (table == null || table.isEmpty()) {
                throw new IllegalArgumentException("table is required");
            }
            return new RowChange(this);
        }
    }
}
