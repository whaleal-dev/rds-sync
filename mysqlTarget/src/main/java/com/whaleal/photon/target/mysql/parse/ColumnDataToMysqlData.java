package com.whaleal.photon.target.mysql.parse;


import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.columntype.java.EnumCommonColumnDataType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


/**
 * Column → MySQL 字面量/值转换。
 */
public class ColumnDataToMysqlData {

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        switch (enumCommonColumnDataType) {
            case NULLCOLUMN:
                return null;
            case BOOLCOLUMN:
                if ((boolean) columnData.getData()) {
                    return 1;
                } else {
                    return 0;
                }
            case INTCOLUMN:
            case SHORTCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case BIGDECIMALCOLUMN:
                return columnData.getData();
            case TIMESTAMPCOLUMN:
            case DATETIMECOLUMN:
                long dateLong = ((long) columnData.getData());
                if (dateLong == 0L) {
                    return null;
                } else if ((dateLong + "").length() > 13) {
                    dateLong = dateLong >> 32;
                }
                return "'" + parseDateTime(new java.util.Date(dateLong)) + "'";
            case STRINGCOLUMN:
            case JSONCOLUMN:
            case ARRAYCOLUMN:
            case PGOBJECTCOLUMN:
            case DATECOLUMN:
            case TIMECOLUMN:
            default:
                return "'" + columnData.getData().toString().replaceAll("'", "\\`") + "'";
        }
    }

    public static String parseDateTime(java.util.Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        return localDateTime.toString();
    }
}
