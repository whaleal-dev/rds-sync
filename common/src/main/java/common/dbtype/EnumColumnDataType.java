package common.dbtype;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumColumnDataType {
    /**
     * 字段类类型
     */
    INTCOLUMN("INTCOLUMN"),
    BIGDECIMALCOLUMN("BIGDECIMALCOLUMN"),
    BLOBCOLUMN("BLOBCOLUMN"),
    LONGCOLUMN("LONGCOLUMN"),
    STRINGCOLUMN("STRINGCOLUMN"),
    DATECOLUMN("DATECOLUMN"),
    DATETIMECOLUMN("DATETIMECOLUMN"),
    ARRAYCOLUMN("ARRAYCOLUMN"),
    DOUBLECOLUMN("DOUBLECOLUMN"),
    FLOATCOLUMN("FLOATCOLUMN"),
    JSONCOLUMN("JSONCOLUMN"),
    TIMESTAMPCOLUMN("TIMESTAMPCOLUMN"),
    OBJECTIDCOLUMN("OBJECTIDCOLUMN"),
    NULLCOLUMN("NULLCOLUMN"),
    SHORTCOLUMN("SHORTCOLUMN"),
    PGOBJECTCOLUMN("PGOBJECTCOLUMN"),
    TIMECOLUN("TIMECOLUN"),
    BYTESCOLUMN("BYTESCOLUMN"),
    BOOLCOLUMN("BOOLCOLUMN");
    private final String upperCase;

    EnumColumnDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






