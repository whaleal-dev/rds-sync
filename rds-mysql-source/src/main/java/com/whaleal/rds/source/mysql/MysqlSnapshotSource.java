package com.whaleal.rds.source.mysql;

import com.whaleal.rds.transfer.model.Identifiers;
import com.whaleal.rds.transfer.model.RowChange;
import com.whaleal.rds.transfer.model.RowChangeSource;
import com.whaleal.rds.transfer.spi.RowChangeListener;
import com.whaleal.rds.transfer.spi.SnapshotSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * MySQL 全量快照：JDBC 扫表产出 {@code RowChange(op=r)}，不经过 PhotonT MemoryCache。
 */
public final class MysqlSnapshotSource implements SnapshotSource {

    private final String jdbcUri;
    private final String user;
    private final String password;
    private final Pattern tableWhite;
    private final int splitSlices;
    private final Object pauseLock = new Object();

    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean complete;
    private volatile Connection connection;

    public MysqlSnapshotSource(String jdbcUri,
                               String user,
                               String password,
                               String tableWhite,
                               int splitSlices) {
        this.jdbcUri = jdbcUri;
        this.user = user;
        this.password = password;
        this.tableWhite = Pattern.compile(tableWhite == null || tableWhite.isEmpty() ? ".*" : tableWhite);
        this.splitSlices = splitSlices <= 0 ? 4 : splitSlices;
    }

    @Override
    public void start(RowChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is required");
        }
        running = true;
        complete = false;
        try {
            connection = MysqlConnections.open(jdbcUri, user, password);
            List<String[]> tables = listTables(connection);
            for (String[] ns : tables) {
                awaitIfPaused();
                if (!running) {
                    return;
                }
                scanTable(connection, ns[0], ns[1], listener);
            }
            complete = running;
        } catch (SQLException e) {
            throw new IllegalStateException("mysql snapshot failed: " + e.getMessage(), e);
        } finally {
            running = false;
            closeQuietly();
        }
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void stop() {
        running = false;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    /** P1 全量可暂停。 */
    public void pause() {
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }

    @Override
    public void close() {
        stop();
        closeQuietly();
    }

    private void awaitIfPaused() {
        synchronized (pauseLock) {
            while (paused && running) {
                try {
                    pauseLock.wait(200L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                    return;
                }
            }
        }
    }

    List<String[]> listTables(Connection conn) throws SQLException {
        List<String[]> out = new ArrayList<String[]>();
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getTables(null, null, "%", new String[] {"TABLE"});
        try {
            while (rs.next()) {
                String schema = rs.getString("TABLE_CAT");
                if (schema == null || schema.isEmpty()) {
                    schema = rs.getString("TABLE_SCHEM");
                }
                String table = rs.getString("TABLE_NAME");
                if (schema == null || table == null) {
                    continue;
                }
                if (isSystemSchema(schema)) {
                    continue;
                }
                String qn = schema + "." + table;
                if (tableWhite.matcher(qn).matches()) {
                    out.add(new String[] {schema, table});
                }
            }
        } finally {
            rs.close();
        }
        return out;
    }

    private void scanTable(Connection conn, String schema, String table, RowChangeListener listener)
            throws SQLException {
        PkMeta pk = primaryKeys(conn, schema, table);
        List<String> wheres = MysqlRangeSplit.whereClauses(
                conn, schema, table, pk.names, pk.types, splitSlices);
        for (String where : wheres) {
            awaitIfPaused();
            if (!running) {
                return;
            }
            String sql = "SELECT * FROM " + Identifiers.qualified(schema, table) + " WHERE " + where;
            PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ResultSet rs = null;
            try {
                rs = ps.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    awaitIfPaused();
                    if (!running) {
                        return;
                    }
                    Map<String, Object> after = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= cols; i++) {
                        after.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    listener.onEvent(RowChange.builder()
                            .op("r")
                            .schema(schema)
                            .table(table)
                            .after(after)
                            .pkColumns(pk.names)
                            .tsMs(System.currentTimeMillis())
                            .source(new RowChangeSource("mysql-snapshot", "true", where))
                            .build());
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
                ps.close();
            }
        }
    }

    private PkMeta primaryKeys(Connection conn, String schema, String table) throws SQLException {
        List<String> names = new ArrayList<String>();
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getPrimaryKeys(schema, null, table);
        try {
            while (rs.next()) {
                names.add(rs.getString("COLUMN_NAME"));
            }
        } finally {
            rs.close();
        }
        if (names.isEmpty()) {
            rs = md.getPrimaryKeys(null, schema, table);
            try {
                while (rs.next()) {
                    names.add(rs.getString("COLUMN_NAME"));
                }
            } finally {
                rs.close();
            }
        }
        int[] types = new int[names.size()];
        ResultSet cols = md.getColumns(schema, null, table, null);
        try {
            Map<String, Integer> typeByName = new LinkedHashMap<String, Integer>();
            while (cols.next()) {
                typeByName.put(cols.getString("COLUMN_NAME"), cols.getInt("DATA_TYPE"));
            }
            for (int i = 0; i < names.size(); i++) {
                Integer t = typeByName.get(names.get(i));
                types[i] = t == null ? 0 : t;
            }
        } finally {
            cols.close();
        }
        return new PkMeta(names, types);
    }

    private static boolean isSystemSchema(String schema) {
        String s = schema.toLowerCase(Locale.ROOT);
        return "mysql".equals(s) || "information_schema".equals(s)
                || "performance_schema".equals(s) || "sys".equals(s);
    }

    private void closeQuietly() {
        Connection c = connection;
        connection = null;
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

    private static final class PkMeta {
        final List<String> names;
        final int[] types;

        PkMeta(List<String> names, int[] types) {
            this.names = names == null ? Collections.<String>emptyList() : names;
            this.types = types == null ? new int[0] : types;
        }
    }
}
