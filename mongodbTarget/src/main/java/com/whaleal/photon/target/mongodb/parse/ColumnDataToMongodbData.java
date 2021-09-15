package com.whaleal.photon.target.mongodb.parse;

import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.column.TimestampColumn;
import com.whaleal.photon.common.common.columntype.java.EnumCommonColumnDataType;
import org.bson.BsonTimestamp;
import org.bson.Document;
import java.sql.Timestamp;
import java.util.Date;



/**
 * @author liheping
 */
public class ColumnDataToMongodbData {
    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        switch (enumCommonColumnDataType) {
            case DATETIMECOLUMN:
                return new Date((long) (columnData.getData()));
            case TIMESTAMPCOLUMN:
                TimestampColumn column = (TimestampColumn) columnData;
                if (column.getSeconds() > 0) {
                    //代表数据从mongodb出来
                    return new BsonTimestamp(column.getSeconds(), column.getInc());
                } else {
                    if (column.getData() > 0) {
                        return new Timestamp(column.getData());
                    }
                }
            case DATECOLUMN:
            case TIMECOLUMN:
            case PGOBJECTCOLUMN:
                return columnData.getData().toString();
            case JSONCOLUMN:
                return Document.parse(columnData.getData().toString());
            case NULLCOLUMN:
                return null;
            case DEFAULTTYPECOLUMN:
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
            default:
                return columnData.getData();
        }
    }

}
