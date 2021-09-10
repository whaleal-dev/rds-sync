package parse;


import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import common.column.AbstractColumn;
import common.dbtype.java.EnumCommonColumnDataType;
import org.bson.Document;

import java.sql.Timestamp;
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
        switch (enumCommonColumnDataType) {
            case DATETIMECOLUMN:
                return new Date((long) (columnData.getData()));
            case TIMESTAMPCOLUMN:
                return new Timestamp((long) (columnData.getData()));
            case DATECOLUMN:
            case TIMECOLUMN:
            case PGOBJECTCOLUMN:
                return columnData.getData().toString();
            case JSONCOLUMN:
                return Document.parse(columnData.getData().toString());
//            case ARRAYCOLUMN:
//                // return gson.fromJson(columnData.getData().toString(), List.class);
//                return JSONObject.parse(columnData.getData());
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
            case MONGODBOBJECTCOLUMN:
            case BOOLCOLUMN:
            case DEFAULTTYPECOLUMN:
            default:
                return columnData.getData();
        }
    }

    public static void main(String[] args) {
        List list = gson.fromJson("[12,12,13]", List.class);
        System.out.println(list);
        Object parse1 = JSONObject.parse("[12,12,13]");
        System.out.println(parse1);
        Document parse = Document.parse("[12,12,13]");
        System.out.println(parse);
    }
}
