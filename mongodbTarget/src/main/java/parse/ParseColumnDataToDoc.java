package parse;


import common.column.*;
import common.dbtype.EnumColumnDataType;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @author liheping
 */
public class ParseColumnDataToDoc {
    private static DateTimeFormatter timestampSimpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.n");

    public static Object parseColumnData(AbstractColumn columnData) {

        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case INTCOLOMN:
            case STRINGCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
                return columnData.getData();
            case DATECOLUMN:
                return LocalDateTime.parse(columnData.getData().toString(), timestampSimpleDateFormat);
            case TIMESTAMPCOLUMN:
                return Long.parseLong(columnData.getData().toString());
            case JSONCOLUMN:
                return Document.parse(columnData.getData().toString());
            default:
                return columnData.getData();
        }
    }

}
