package parse;


import common.column.AbstractColumn;
import common.dbtype.java.EnumCommonColumnDataType;


/**
 * @author liheping
 */
public class ColumnDataToMysqlData {

    public static Object parseColumnData(AbstractColumn columnData) {
        String type = columnData.getClass().getSimpleName().toUpperCase();
        EnumCommonColumnDataType enumCommonColumnDataType = EnumCommonColumnDataType.valueOf(type);
        // blob没解析 bytes 时间类型的转换
        switch (enumCommonColumnDataType) {
            case NULLCOLUMN:
                return null;
            case BOOLCOLUMN:
                if ((boolean) columnData.getData()) {
                    return 1;
                } else {
                    return 0;
                }
            case INTCOLUMN:
            case SHORTCOLUMN:
            case LONGCOLUMN:
            case DOUBLECOLUMN:
            case FLOATCOLUMN:
            case BIGDECIMALCOLUMN:
                return columnData.getData();
//            case DATECOLUMN:
//
//            case TIMECOLUN:
//            case TIMESTAMPCOLUMN:
////                return columnData.getData();
//                return null;
//            case DATETIMECOLUMN:
//                return "'" + new Timestamp((long) columnData.getData()) + "'";
            case STRINGCOLUMN:
            case OBJECTIDCOLUMN:
            case JSONCOLUMN:
            case ARRAYCOLUMN:
            case PGOBJECTCOLUMN:
            default:
                return "'" + columnData.getData().toString().replaceAll("'", "\\`") + "'";
        }
    }

}
