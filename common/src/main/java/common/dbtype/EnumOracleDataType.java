package common.dbtype;

/**
 * Oracle数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumOracleDataType {

    /**
     * oracle类型
     */
    BFILE("BFILE"),
    BIGDECIMAL("BIGDECIMAL"),
    BINARY_DOUBLE("BINARY_DOUBLE"),
    BINARY_FLOAT("BINARY_FLOAT"),
    BLOB("BLOB"),
    CHAR("CHAR"),
    CHARVARYING("CHAR VARYING"),
    CHARACTER("CHARACTER"),
    CHARACTERVARYING("CHARACTER VARYING"),
    CLOB("CLOB"),
    DATE("DATE"),
    DECIMAL("DECIMAL"),
    DOUBLE_PRECISION("DOUBLE PRECISION"),
    FLOAT("FLOAT"),
    INT("INT"),
    INTEGER("INTEGER"),
    LONG("LONG"),
    NCHAR("NCHAR"),
    SMALLINT("SMALLINT"),
    STRING("STRING"),
    TIMESTAMP("TIMESTAMP"),
    VARCHAR("VARCHAR"),
    VARCHAR2("VARCHAR2"),
    NUMBERIC("NUMBERIC"),
    NUMBER("NUMBER");
    private final String upperCase;

    EnumOracleDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }

}
