package common.dbtype;

/**
 * MySQL数据类型类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public enum EnumMySqlDataType {
    /**
     * Long
     */
    LONG("LONG"),
    /**
     * String
     */
    STRING("STRING"),
    /**
     * TINYINT 短int
     */
    TINYINT("TINYINT"),
    /**
     * SMALLINT 小int
     */
    SMALLINT("SMALLINT"),
    /**
     * MEDIUMINT 中等int
     */
    MEDIUMINT("MEDIUMINT"),
    /**
     * INT int
     */
    INT("INT"),
    /**
     * INTEGER integer
     */
    INTEGER("INTEGER"),
    /**
     * BIGINT 长int
     */
    BIGINT("BIGINT"),
    /**
     * FLOAT 单浮点型
     */
    FLOAT("FLOAT"),
    /**
     * DOUBLE 双浮点型
     */
    DOUBLE("DOUBLE"),
    /**
     * DECIMAL decimal
     */
    DECIMAL("DECIMAL"),
    /**
     * DATE 日期
     *
     * @desc 年月日
     */
    DATE("DATE"),
    /**
     * TIME 时间
     *
     * @desc 时分秒
     */
    TIME("TIME"),
    /**
     * YEAR 年
     *
     * @desc 年
     */
    YEAR("YEAR"),
    /**
     * DATETIME 日期时间
     *
     * @desc 年月日 时分秒
     */
    DATETIME("DATETIME"),
    /**
     * TIMESTAMP 时间戳
     */
    TIMESTAMP("TIMESTAMP"),
    /**
     * CHAR char
     */
    CHAR("CHAR"),
    /**
     * VARCHAR 可变char
     */
    VARCHAR("VARCHAR"),
    /**
     * TINYBLOB 短blob
     */
    TINYBLOB("TINYBLOB"),
    /**
     * TINYTEXT 短text
     */
    TINYTEXT("TINYTEXT"),
    /**
     * BLOB blob
     */
    BLOB("BLOB"),
    /**
     * TEXT text
     */
    TEXT("TEXT"),
    /**
     * MEDIUMBLOB 中等blob
     */
    MEDIUMBLOB("MEDIUMBLOB"),
    /**
     * MEDIUMTEXT 中等text
     */
    MEDIUMTEXT("MEDIUMTEXT"),
    /**
     * LONGBLOB 长blob
     */
    LONGBLOB("LONGBLOB"),
    /**
     * LONGTEXT 长text
     */
    LONGTEXT("LONGTEXT");

    private final String upperCase;

    EnumMySqlDataType(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }


}






