package common.dbtype;

import org.bson.conversions.Bson;

/**
 * MongoDB数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMongoDbDataType {
    /**
     * mongodb类型
     */
    BOOLEAN("BOOLEAN"),
    INTEGER("INTEGER"),
    LONG("LONG"),
    DOUBLE("DOUBLE"),
    DECIMAL128("DECIMAL128"),
    ARRAYLIST("ARRAYLIST"),
    DOCUMENT("DOCUMENT"),
    CODE("CODE"),
    REGULAR("BSONREGULAREXPRXSSION"),
    OBJECTID("OBJECTID"),
    STRING("STRING"),
    BSONTIMESTAMP("BSONTIMESTAMP"),
    DATE("DATE");

    private final String upperCase;
    EnumMongoDbDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






