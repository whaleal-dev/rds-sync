package common.dbtype.java;

import org.postgresql.util.PGInterval;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumPgDataInJavaType {
    /**
     * pg在java中的类型类型
     */
    //为pgObject的统称类
    PGOBJECT("PGOBJECT"),
    STRING("STRING"),
    LONG("LONG"),
    DOUBLE("DOUBLE"),
    INTEGER("INTEGER"),
    BIGDECIMAL("BIGDECIMAL"),
    FLOAT("FLOAT"),
    TIME("TIME"),
    TIMESTAMP("TIMESTAMP"),
    BOOLEAN("BOOLEAN"),
    BYTES("BYTES"),
    DATE("DATE"),
    PGPOINT("PGPOINT"),
    PGLINE("PGLINE"),
    PGLSEG("PGLSEG"),
    PGBOX("PGBOX"),
    PGPATH("PGPATH");


    private final String upperCase;

    EnumPgDataInJavaType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


    public static void main(String[] args) {
        Byte[] bytes = new Byte[]{1, 0};
        System.out.println(bytes.getClass().getSimpleName());
    }
}






