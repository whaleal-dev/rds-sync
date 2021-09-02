package common.dbtype;

/**
 * Oracle数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumOracleDataType {

    BIGDECIMAL("BIGDECIMAL"),
    BFILE("BFILE"),
    DOUBLE("DOUBLE"),
    FLOAT("FLOAT"),
    STRING("STRING"),
    TIMESTAMP("TIMESTAMP"),
    BYTES("BYTE[]");

    private final String upperCase;

    EnumOracleDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }

}
