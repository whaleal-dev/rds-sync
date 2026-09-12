package com.whaleal.rds.source.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

final class MysqlConnections {

    private MysqlConnections() {
    }

    static Connection open(String uri, String user, String password) throws SQLException {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("jdbc uri is required");
        }
        Properties props = new Properties();
        if (user != null && !user.isEmpty()) {
            props.setProperty("user", user);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        props.setProperty("useSSL", "false");
        props.setProperty("characterEncoding", "utf8");
        return DriverManager.getConnection(uri, props);
    }
}
