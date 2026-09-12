package com.whaleal.rds.common.common.columntype.java;

/**
 * MySQL数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMySqlDataInJavaType {
    /**
     * 字符串
     */
    STRING("STRING"),
    /**
     * 整型
     */
    INTEGER("INTEGER"),
    /**
     * 长整型
     */
    LONG("LONG"),
    /**
     * float
     */
    FLOAT("FLOAT"),
    /**
     * double
     */
    DOUBLE("DOUBLE"),
    /**
     * decimal
     */
    BIGDECIMAL("BIGDECIMAL"),
    /**
     * 日期 年月日
     */
    DATE("DATE"),
    /**
     * 时间 时分秒
     */
    TIME("TIME"),
    /**
     * 时间戳
     */
    TIMESTAMP("TIMESTAMP"),
    /**
     * 本地日期时间
     */
    LOCALDATETIME("LOCALDATETIME"),
    /**
     * 二进制组byte[]
     */
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






