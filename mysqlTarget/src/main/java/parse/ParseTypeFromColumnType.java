package parse;

import common.column.AbstractColumn;
import common.columnclass.ColumnType;
import common.dbtype.java.EnumCommonColumnDataType;
import common.dbtype.db.EnumMySqlDataType;
import common.dbtype.MySqlType;

/**
 * 解析mongodb数据到mysql类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class ParseTypeFromColumnType {

    public static ColumnType parseType(AbstractColumn columnData) {
        ColumnType columnType = new ColumnType();
        columnType.setColumnName(columnData.getColumnName());
        int objectLength = columnData.toString().length();
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        switch (enumCommonColumnDataType) {
            case BOOLCOLUMN:
                columnType.setColumnType(MySqlType.TINYINT);
                columnType.setLength(4);
                break;
            case SHORTCOLUMN:
                columnType.setColumnType(MySqlType.TINYINT);
                columnType.setLength(objectLength);
                break;
            case INTCOLUMN:
                columnType.setColumnType(MySqlType.INT);
                break;
            case BIGDECIMALCOLUMN:
                columnType.setColumnType(MySqlType.DECIMAL);
                break;
            case LONGCOLUMN:
                columnType.setColumnType(MySqlType.BIGINT);
                break;
            case DOUBLECOLUMN:
                columnType.setColumnType(MySqlType.DOUBLE);
                columnType.setPrecision(0);
                if (columnData.getData().toString().contains(".")) {
                    columnType.setPrecision(columnData.getData().toString().split("\\.")[1].length());
                }
                columnType.setLength(columnData.getData().toString().length());
                break;
            case FLOATCOLUMN:
                columnType.setColumnType(MySqlType.FLOAT);
                columnType.setPrecision(0);
                if (columnData.getData().toString().contains(".")) {
                    columnType.setPrecision(columnData.getData().toString().split("\\.")[1].length());
                }
                columnType.setLength(columnData.getData().toString().length());
                break;
            case OBJECTIDCOLUMN:
                columnType.setColumnType(MySqlType.CHAR);
                columnType.setLength(objectLength);
                break;
            case DATECOLUMN:
                columnType.setColumnType(MySqlType.DATE);
                break;
            case TIMECOLUN:
                columnType.setColumnType(MySqlType.TIME);
                break;
            case DATETIMECOLUMN:
                columnType.setColumnType(MySqlType.DATETIME);
                break;
            case TIMESTAMPCOLUMN:
                columnType.setColumnType(MySqlType.TIMESTAMP);
                break;
            case BYTESCOLUMN:
                // 需要优化
                columnType.setColumnType(MySqlType.BLOB);
                break;
            case JSONCOLUMN:
            case STRINGCOLUMN:
            case ARRAYCOLUMN:
            case PGOBJECTCOLUMN:
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
        dataLength = (int) (dataLength * 2);
        if (dataLength < 10000) {
            columnType.setColumnType(MySqlType.VARCHAR);
            columnType.setLength(dataLength);
        } else if (dataLength < 16777215) {
            columnType.setColumnType(MySqlType.MEDIUMTEXT);
        } else {
            columnType.setColumnType(MySqlType.LONGTEXT);
        }
    }


    public static boolean isModifyTypeOrLength(AbstractColumn columnValue, ColumnType columnType) {
        String oldValueType = columnType.getColumnType().toUpperCase();
        int oldValueLength = columnType.getLength();
        int newValueLength = columnValue.getData().toString().length();
        boolean isAlter = false;
        ColumnType columnTypeTemp = null;
        EnumMySqlDataType enumMySqlDataType = EnumMySqlDataType.valueOf(oldValueType);
        switch (enumMySqlDataType) {
            case BIGINT:
            case INT:
            case TINYINT:
            case CHAR:
            case VARCHAR:
                if (newValueLength > oldValueLength) {
                    isAlter = true;
                }
                break;
            case DOUBLE:
            case DECIMAL:
            case FLOAT: {
                columnTypeTemp = ParseTypeFromColumnType.parseType(columnValue);
                if (columnValue.getData().toString().contains(".")) {
                    int oldPrecision = columnType.getPrecision();
                    int oldIntLength = columnType.getLength() - columnType.getPrecision();
                    int newPrecision = columnValue.getData().toString().split("\\.")[1].length();
                    int newIntLength = columnValue.getData().toString().split("\\.")[0].length();
                    if (newIntLength > oldIntLength) {
                        columnTypeTemp.setLength(newIntLength + oldPrecision);
                        columnTypeTemp.setPrecision(oldPrecision);
                        isAlter = true;
                    }
                    if (newPrecision > oldPrecision) {
                        int columnLengthTemp = columnTypeTemp.getLength();
                        if (columnLengthTemp < oldIntLength + newPrecision) {
                            columnTypeTemp.setLength(oldIntLength + newPrecision);
                            isAlter = true;
                        }
                    }
                } else {
                    if (newValueLength > oldValueLength) {
                        isAlter = true;
                    }
                }
            }
            break;
            default:
                isAlter = false;
        }
        return isAlter;
    }

}