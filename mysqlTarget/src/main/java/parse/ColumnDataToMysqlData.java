package parse;


import common.column.AbstractColumn;
import common.dbtype.java.EnumCommonColumnDataType;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


/**
 * @author liheping
 */
public class ColumnDataToMysqlData {

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        // blob没解析 bytes 时间类型的转换
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
                if(dateLong==0L){
                    return null;
                }
                return "'" + parseDateTime(new java.util.Date(dateLong)) + "'";
            case STRINGCOLUMN:
            case OBJECTIDCOLUMN:
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
        System.out.println(date.toString());
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime localDateTime = instant.atZone(zoneId).toLocalDateTime();
        System.out.println("Date = " + date);
        System.out.println("LocalDateTime = " + localDateTime);
        return localDateTime.toString();
    }

    public static void main(String[] args) {
        Time time = new Time(0);
        System.out.println(time.toString());
    }
}
