package parse;

import com.google.gson.Gson;
import common.column.*;
import common.dbtype.EnumMySqlDataType;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 转换mysql数据的列
 *
 * @author: jy
 * @Date: 2021/08/26
 */
public class TransformationMysqlDataToColumn {

    private static final Gson gson = new Gson();

    /**
     * us时间格式
     */
    private final static DateTimeFormatter formatterOfUs = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
    /**
     * 中国时间格式
     */
    private final static DateTimeFormatter formatterOfZh = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    public static AbstractColumn parseValue(String columnName, Object object) {
        if (object == null) {
            return new StringColumn(columnName, "null");
        }
        //获取 mysql 值的数据类型
        String type = object.getClass().getSimpleName().toUpperCase();
        EnumMySqlDataType enumMySqlDataType = EnumMySqlDataType.valueOf(type);
        switch (enumMySqlDataType) {
            case LONG:
            case BIGINT:
            case MEDIUMINT:
                return new LongColumn(columnName, (Long) object);
            case INT:
            case TINYINT:
            case SMALLINT:
            case INTEGER:
                return new IntColumn(columnName, (Integer) object);
            case FLOAT:
                return new FloatColumn(columnName, (Float) object);
            case DOUBLE:
                return new DoubleColumn(columnName, (Double) object);
            case DECIMAL:
                return new BigDecimalColumn(columnName, (BigDecimal) object);
            case DATE:
                return new DateColumn(columnName, (String) object);
            case TIME:
            case TIMESTAMP:
                return new TimestampColumn(columnName, ((Timestamp) object).getTime());
            case DATETIME:
                return new DateTimeColumn(columnName, (Long) object);
            case TINYBLOB:
            case BLOB:
            case MEDIUMBLOB:
            case LONGBLOB:
                return new BlobColumn(columnName, (Blob) object);
            case STRING:
            case YEAR:
            case TEXT:
            case TINYTEXT:
            case MEDIUMTEXT:
            case LONGTEXT:
            case CHAR:
            case VARCHAR:
                return new StringColumn(columnName, object.toString());
            default:
                return new StringColumn(columnName, object.toString());
        }
    }
}
