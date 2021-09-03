package common.dbtype.java;

/**
 * MySQL数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMySqlDataInJavaType {
    /**
     *
     */
    STRING("STRING"),
    INTEGER("INTEGER"),
    LONG("LONG"),
    FLOAT("FLOAT"),
    DOUBLE("DOUBLE"),
    BIGDECIMAL("BIGDECIMAL"),
    DATE("DATE"),
    TIME("TIME"),
    TIMESTAMP("TIMESTAMP"),
    BYTES("BYTES");

    private final String upperCase;

    EnumMySqlDataInJavaType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






