package common.dbtype.java;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumCommonColumnDataType {
    /**
     * int类型
     */
    INTCOLUMN("INTCOLUMN"),

    TIMECOLUMN("TIMECOLUMN"),
    /**
     * decimal类型
     */
    BIGDECIMALCOLUMN("BIGDECIMALCOLUMN"),
    /**
     * blob类型 本质是byte[]
     */
    BLOBCOLUMN("BLOBCOLUMN"),
    /**
     * long类型
     */
    LONGCOLUMN("LONGCOLUMN"),
    /**
     * 字符串类型
     */
    STRINGCOLUMN("STRINGCOLUMN"),
    /**
     * 日期类型 年月日
     */
    DATECOLUMN("DATECOLUMN"),
    /**
     * 日期时间类型 年月日日分秒毫秒
     */
    DATETIMECOLUMN("DATETIMECOLUMN"),
    /**
     * list集合类型
     */
    ARRAYCOLUMN("ARRAYCOLUMN"),
    /**
     * double类型
     */
    DOUBLECOLUMN("DOUBLECOLUMN"),
    /**
     * float类型
     */
    FLOATCOLUMN("FLOATCOLUMN"),
    /**
     * json类型
     */
    JSONCOLUMN("JSONCOLUMN"),
    /**
     * 时间戳类型
     */
    TIMESTAMPCOLUMN("TIMESTAMPCOLUMN"),

    /**
     * null类型
     */
    NULLCOLUMN("NULLCOLUMN"),
    /**
     * short类型
     */
    SHORTCOLUMN("SHORTCOLUMN"),
    /**
     * 布尔类型
     */
    BOOLCOLUMN("BOOLCOLUMN"),

    /**
     * bytes类型
     */
    BYTESCOLUMN("BYTESCOLUMN"),
    /**
     * mongodb类型
     */
    MONGODBOBJECTCOLUMN("MONGODBOBJECTCOLUMN"),
    /**
     * pg的对象类型
     */
    PGOBJECTCOLUMN("PGOBJECTCOLUMN"),
    DEFAULTTYPECOLUMN("DEFAULTTYPECOLUMN");


    private final String upperCase;

    EnumCommonColumnDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






