package com.whaleal.photon.common.common.columntype.java;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMongoDbDataInJavaType {
    /**
     * ObjectId
     */
    OBJECTID("OBJECTID"),
    /**
     * Double
     */
    DOUBLE("DOUBLE"),
    /**
     * String字符串
     */
    STRING("STRING"),
    /**
     * document对象
     */
    DOCUMENT("DOCUMENT"),
    /**
     * 整型
     */
    INTEGER("INTEGER"),
    /**
     * 二进制
     */
    BINARY("BINARY"),
    /**
     * 数组
     */
    ARRAYLIST("ARRAYLIST"),
    /**
     * 布尔
     */
    BOOLEAN("BOOLEAN"),
    /**
     * 时间。年月日时分秒 iso
     */
    DATE("DATE"),
    /**
     * 正则表达式
     */
    BSONREGULAREXPRESSION("BSONREGULAREXPRESSION"),
    /**
     * decimal
     */
    DECIMAL128("DECIMAL128"),
    /**
     * 点对象
     */
    BSONDBPOINTER("BSONDBPOINTER"),
    /**
     * 未定义对象
     */
    BSONUNDEFINED("BSONUNDEFINED"),
    /**
     * 代码类型
     */
    CODE("CODE"),
    /**
     * 不知道
     */
    SYMBOL("SYMBOL"),
    /**
     * 代码类型
     */
    CODEWITHSCOPE("CODEWITHSCOPE"),
    /**
     * 长整型
     */
    LONG("LONG"),
    /**
     * MinKey
     */
    MINKEY("MINKEY"),
    /**
     * MaxKey
     */
    MAXKEY("MAXKEY"),
    /**
     * 时间戳类型
     */
    BSONTIMESTAMP("BSONTIMESTAMP");

    private final String upperCase;

    EnumMongoDbDataInJavaType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






