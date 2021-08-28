package parse;


import common.column.AbstractColumn;
import common.dbtype.EnumColumnDataType;
import org.bson.Document;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * @author liheping
 */
public class ParseColumnDataToMysql {
    private static DateTimeFormatter timestampSimpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSz");

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case NULLCOLUMN:
                return null;
            case BOOLCOLUMN:
                if ((boolean) columnData.getData()) {
                    return 1;
                } else {
                    return 0;
                }
            case INTCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case TIMESTAMPCOLUMN:
//                return columnData.getData();
                return null;
            case DATETIMECOLUMN:
                return "'" + new Timestamp((long) columnData.getData()) + "'";
            case DATECOLUMN:
            case STRINGCOLUMN:
            case OBJECTIDCOLUMN:
            case JSONCOLUMN:
            case ARRAYCOLUMN:
            default:
                return "'" + columnData.getData().toString().replaceAll("'", "\\`") + "'";
        }
    }

}
