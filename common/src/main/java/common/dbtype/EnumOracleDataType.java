package common.dbtype;

/**
 * Oracle数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumOracleDataType {

    /**
     * mongodb类型
     */
    CHAR("CHAR"),
    VARCHAR2("VARCHAR2"),
    NUMBER("NUMBER"),
    DATE("DATE"),
    BINARY_DOUBLE("BINARY_DOUBLE"),
    BINARY_FLOAT("BINARY_FLOAT"),
    LONG("LONG");

    private final String upperCase;

    EnumOracleDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }

}
