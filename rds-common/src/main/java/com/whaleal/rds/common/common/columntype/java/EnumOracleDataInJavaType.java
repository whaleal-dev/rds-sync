package com.whaleal.rds.common.common.columntype.java;

/**
 * Oracle数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumOracleDataInJavaType {

    /**
     *
     */
    BIGDECIMAL("BIGDECIMAL"),
    BFILE("BFILE"),
    DOUBLE("DOUBLE"),
    FLOAT("FLOAT"),
    INTEGER("INTEGER"),
    STRING("STRING"),
    TIMESTAMP("TIMESTAMP"),
    BYTES("BYTES");

    private final String upperCase;

    EnumOracleDataInJavaType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }

}
