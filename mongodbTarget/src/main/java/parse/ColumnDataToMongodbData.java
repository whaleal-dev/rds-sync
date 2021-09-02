package parse;


import com.google.gson.Gson;
import common.column.*;
import common.dbtype.java.EnumCommonColumnDataType;
import org.bson.BsonTimestamp;
import org.bson.Document;

import java.util.Date;
import java.util.List;


/**
 * @author liheping
 */
public class ColumnDataToMongodbData {
    private static Gson gson = new Gson();

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        // 时间类型可能有问题
        switch (enumCommonColumnDataType) {
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
