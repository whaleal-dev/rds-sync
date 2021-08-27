package parse;


import common.column.AbstractColumn;
import common.dbtype.EnumColumnDataType;

import java.time.format.DateTimeFormatter;


/**
 * @author liheping
 */
public class ParseColumnDataToMysql {
    private static DateTimeFormatter timestampSimpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.n");

    public static Object parseColumnData(AbstractColumn columnData) {

        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case INTCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case TIMESTAMPCOLUMN:
                return Long.parseLong(columnData.getData().toString());
            case STRINGCOLUMN:
            case OBJECTIDCOLUMN:
            case DATECOLUMN:
            case JSONCOLUMN:
            default:
                return "'" + columnData.getData().toString().replaceAll("'", "\\`") + "'";
        }
    }

}
