package com.whaleal.rds.source.mysql;

import com.whaleal.rds.common.common.dataclass.Range;
import com.whaleal.rds.common.util.split.RangeSplitUtil;
import com.whaleal.rds.transfer.model.Identifiers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数值主键 Range 切分；否则整表扫描。复用 PhotonT {@link RangeSplitUtil}。
 */
final class MysqlRangeSplit {

    private MysqlRangeSplit() {
    }

    static List<String> whereClauses(Connection conn,
                                     String schema,
                                     String table,
                                     List<String> pkColumns,
                                     int[] pkTypes,
                                     int slices) throws SQLException {
        if (pkColumns.size() == 1 && isIntegral(pkTypes[0])) {
            String pk = pkColumns.get(0);
            long[] minMax = minMax(conn, schema, table, pk);
            if (minMax != null) {
                List<Range> ranges = RangeSplitUtil.getRangeListByLongType(
                        minMax[0], minMax[1], Math.max(1, slices), pk);
                List<String> wheres = new ArrayList<String>();
                for (Range range : ranges) {
                    if (range.getQuery() != null) {
                        wheres.add(String.valueOf(range.getQuery()));
                    }
                }
                if (!wheres.isEmpty()) {
                    return wheres;
                }
            }
        }
        return Collections.singletonList("1=1");
    }

    private static long[] minMax(Connection conn, String schema, String table, String pk)
            throws SQLException {
        String sql = "SELECT MIN(" + Identifiers.mysqlQuote(pk) + "), MAX("
                + Identifiers.mysqlQuote(pk) + ") FROM " + Identifiers.qualified(schema, table);
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = null;
        try {
            rs = ps.executeQuery();
            if (!rs.next()) {
                return null;
            }
            Object min = rs.getObject(1);
            Object max = rs.getObject(2);
            if (min == null || max == null) {
                return null;
            }
            return new long[] {((Number) min).longValue(), ((Number) max).longValue()};
        } finally {
            if (rs != null) {
                rs.close();
            }
            ps.close();
        }
    }

    static boolean isIntegral(int sqlType) {
        return sqlType == Types.TINYINT || sqlType == Types.SMALLINT
                || sqlType == Types.INTEGER || sqlType == Types.BIGINT;
    }
}
