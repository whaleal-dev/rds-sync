package parse;

import common.column.AbstractColumn;
import common.columnclass.ColumnType;
import common.dbtype.EnumColumnDataType;
import common.dbtype.MySqlType;
import org.bson.Document;

import java.time.LocalDateTime;

/**
 * 解析mongodb数据到mysql类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class ParseTypeFromColumn {

    public static ColumnType parseType(AbstractColumn columnData) {
        ColumnType columnType = new ColumnType();
        columnType.setColumnName(columnData.getColumnName());
        int objectLength = columnData.toString().length();
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumColumnDataType enumColumnDataType = EnumColumnDataType.valueOf(type);
        switch (enumColumnDataType) {
            case INTCOLUMN:
                columnType.setColumnType(MySqlType.INT);
                break;
            case LONGCOLUMN:
                columnType.setColumnType(MySqlType.BIGINT);
                break;
            case DOUBLECOLUMN:
                columnType.setColumnType(MySqlType.DOUBLE);
                break;
            case FLOATCOLUMN:
                columnType.setColumnType(MySqlType.FLOAT);
                break;
            case OBJECTIDCOLUMN:
                columnType.setColumnType(MySqlType.CHAR);
                columnType.setLength(objectLength);
                break;
            case DATECOLUMN:
                columnType.setColumnType(MySqlType.DATETIME);
                break;
            case TIMESTAMPCOLUMN:
                columnType.setColumnType(MySqlType.TIMESTAMP);
                break;
            case JSONCOLUMN:
            case STRINGCOLUMN:
            default:
                columnType.setColumnType(MySqlType.VARCHAR);
                dealStringType(objectLength, columnType);
                break;
        }
        return columnType;
    }

    /**
     * dealStringType 字段类型需要判断长度大小和类型之类的操作
     *
     * @param dataLength
     * @param columnType
     * @desc 字段类型需要判断长度大小和类型之类的操作
     */
    public static void dealStringType(int dataLength, ColumnType columnType) {
        dataLength = (int) (dataLength * 1.3);
        if (dataLength < 65535) {
            columnType.setColumnType(MySqlType.VARCHAR);
            columnType.setLength(dataLength);
        } else if (dataLength < 16777215) {
            columnType.setColumnType(MySqlType.MEDIUMTEXT);
        } else {
            columnType.setColumnType(MySqlType.LONGTEXT);
        }
    }
}
