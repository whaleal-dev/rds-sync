package common.columnclass;

import common.dbtype.DbTypeFlag;
import lombok.*;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 *  字段描述类
 * @author lhp
 * @time 2021-05-31 13:12:12
 * @desc toString函数生成建表字段
 */
public class ColumnType implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 字段名
     */
    private String columnName;
    /**
     * 数据源名称
     */
    private String dsName;
    /**
     * 库表名称
     */
    private String dbTableName;
    /**
     * 字段类型
     */
    private String columnType;
    /**
     * 类型长度
     */
    private int length = -1;
    /**
     * 类型精度
     */
    private int precision = -1;
    /**
     * 是否为空
     */
    private boolean nullAble;
    /**
     * 是否是索引
     */
    private boolean indexAble;

    /**
     * 数据类型
     */
    private String descType;

    public ColumnType(String columnName, String columnType) {
        this.columnName = columnName;
        this.columnType = columnType;
    }

    public ColumnType(String columnName, String columnType, int length) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.length = length;
    }

    public ColumnType(String columnName, String columnType, int length, int precision) {
        this.columnName = columnName;
        this.columnType = columnType;
        this.length = length;
        this.precision = precision;
    }

    public String getColumnType() {
        return columnType.toUpperCase();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("`" + columnName + "`");
        stringBuilder.append(" " + columnType);
        if (length > -1) {
            stringBuilder.append("(" + length);
            if (precision > -1) {
                stringBuilder.append("," + precision + ")");
            } else {
                stringBuilder.append(")");
            }
        }
        if(columnType.equalsIgnoreCase("timestamp")){
            // 默认时间戳精确度为毫秒级
            stringBuilder.append("(6)");
        }
        if (descType != null && DbTypeFlag.ORACLE.equalsIgnoreCase(descType)) {
            return stringBuilder.toString().replaceAll("`", "\"");
        }
        return stringBuilder.toString();
    }

}
