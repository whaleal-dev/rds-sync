package parse;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import common.column.*;
import common.dbtype.EnumColumnDataType;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @author liheping
 */
public class ParseColumnDataToMongodbData {
    private static DateTimeFormatter timestampSimpleDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSz");
    private static Gson gson = new Gson();

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case DATETIMECOLUMN:
                return new Date((long) columnData.getData());
            case TIMESTAMPCOLUMN:
                return new BsonTimestamp((long) (columnData.getData()));
            case DATECOLUMN:
                return columnData.getData().toString();
            case TIMECOLUN:
                return new Date((long) (columnData.getData()));
            case PGOBJECTCOLUMN:
                return columnData.getData().toString();
            case JSONCOLUMN:
                return Document.parse(columnData.getData().toString());
            case ARRAYCOLUMN:
                return gson.fromJson(columnData.getData().toString(), List.class);
            case NULLCOLUMN:
                return null;
            case INTCOLUMN:
            case SHORTCOLUMN:
            case BIGDECIMALCOLUMN:
            case BLOBCOLUMN:
            case STRINGCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case OBJECTIDCOLUMN:
            case BOOLCOLUMN:
            default:
                return columnData.getData();
        }
    }

}
