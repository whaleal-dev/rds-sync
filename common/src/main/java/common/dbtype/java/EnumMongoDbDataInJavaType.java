package common.dbtype.java;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMongoDbDataInJavaType {
    /**
     * mongodb类型
     */
    OBJECTID("OBJECTID"),
    DOUBLE("DOUBLE"),
    STRING("STRING"),
    DOCUMENT("DOCUMENT"),
    INTEGER("INTEGER"),
    BINARY("BINARY"),
    ARRAYLIST("ARRAYLIST"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    BSONREGULAREXPRESSION("BSONREGULAREXPRESSION"),
    DECIMAL128("DECIMAL128"),
    BSONDBPOINTER("BSONDBPOINTER"),
    BSONUNDEFINED("BSONUNDEFINED"),
    CODE("CODE"),
    SYMBOL("SYMBOL"),
    CODEWITHSCOPE("CODEWITHSCOPE"),
    LONG("LONG"),
    MINKEY("MINKEY"),
    MAXKEY("MAXKEY"),
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






