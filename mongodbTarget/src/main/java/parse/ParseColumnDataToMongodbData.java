package parse;


import common.column.*;
import common.dbtype.EnumColumnDataType;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


/**
 * @author liheping
 */
public class ParseColumnDataToMongodbData {
    private static DateTimeFormatter timestampSimpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSz");

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case DATETIMECOLUMN:
                return new Date((long)columnData.getData());
            case TIMESTAMPCOLUMN:
                return new BsonTimestamp(Long.parseLong(columnData.getData().toString()));
            case JSONCOLUMN:
                return Document.parse(columnData.getData().toString());
            case ARRAYCOLUMN:
            case INTCOLUMN:
            case STRINGCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case OBJECTIDCOLUMN:
            case BOOLCOLUMN:
            case DATECOLUMN:
            default:
                return columnData.getData();
        }
    }

}
