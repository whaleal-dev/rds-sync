package parse;


import com.google.gson.Gson;
import common.column.*;
import org.bson.BsonRegularExpression;
import org.bson.Document;
import org.bson.types.Code;
import org.bson.types.Decimal128;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;


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
