package parse;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumColumnDataType {
    /**
     * mongodb类型
     */
    INTCOLOMN("INTCOLOMN"),
    LONGCOLUMN("LONGCOLUMN"),
    STRINGCOLUMN("STRINGCOLUMN"),
    DATECOLUMN("DATECOLUMN"),
    DOUBLECOLUMN("DOUBLECOLUMN"),
    FLOATCOLUMN("FLOATCOLUMN"),
    JSONCOLUMN("JSONCOLUMN"),
    TIMESTAMPCOLUMN("TIMESTAMPCOLUMN"),
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






