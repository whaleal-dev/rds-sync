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
    private static Gson gson = new Gson();

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        // 时间类型可能有问题
        switch (enumColumnDataType) {
            case DATETIMECOLUMN:
            case TIMECOLUN:
                return new Date((long) columnData.getData());
            case TIMESTAMPCOLUMN:
                return new BsonTimestamp((long) (columnData.getData()));
            case DATECOLUMN:
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
