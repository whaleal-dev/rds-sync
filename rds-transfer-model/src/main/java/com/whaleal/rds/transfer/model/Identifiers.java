package com.whaleal.rds.transfer.model;

/**
 * MySQL 标识符引用。非法字符直接拒绝，避免拼进 SQL。
 */
public final class Identifiers {

    private Identifiers() {
    }

    public static String mysqlQuote(String ident) {
        if (ident == null || ident.isEmpty()) {
            throw new IllegalArgumentException("identifier is required");
        }
        for (int i = 0; i < ident.length(); i++) {
            char c = ident.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')
                    && !(c >= '0' && c <= '9') && c != '_') {
                throw new IllegalArgumentException("illegal identifier: " + ident);
            }
        }
        return "`" + ident + "`";
    }

    public static String qualified(String schema, String table) {
        if (schema == null || schema.isEmpty()) {
            return mysqlQuote(table);
        }
        return mysqlQuote(schema) + "." + mysqlQuote(table);
    }
}
