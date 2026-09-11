package com.whaleal.rds.transfer.model;

/**
 * DDL 事件。Pipeline 在应用前须 barrier 排空相关在途 CRUD。
 */
public final class DdlEvent {

    public enum Type {
        CREATE_TABLE,
        ALTER_TABLE,
        DROP_TABLE,
        RENAME_TABLE,
        TRUNCATE_TABLE,
        CREATE_INDEX,
        DROP_INDEX,
        OTHER
    }

    private final Type type;
    private final String schema;
    private final String table;
    private final String sql;
    private final Long tsMs;

    private DdlEvent(Builder b) {
        this.type = b.type;
        this.schema = b.schema;
        this.table = b.table;
        this.sql = b.sql;
        this.tsMs = b.tsMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Type getType() {
        return type;
    }

    public String getSchema() {
        return schema;
    }

    public String getTable() {
        return table;
    }

    public String getSql() {
        return sql;
    }

    public Long getTsMs() {
        return tsMs;
    }

    public static final class Builder {
        private Type type = Type.OTHER;
        private String schema;
        private String table;
        private String sql;
        private Long tsMs;

        public Builder type(Type type) {
            this.type = type == null ? Type.OTHER : type;
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

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder tsMs(Long tsMs) {
            this.tsMs = tsMs;
            return this;
        }

        public DdlEvent build() {
            return new DdlEvent(this);
        }
    }
}
