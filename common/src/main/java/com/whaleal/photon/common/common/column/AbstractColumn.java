package com.whaleal.photon.common.common.column;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @desc: 抽象字段类
 * @author: lhp
 * @time: 2021/8/23 9:53 上午
 */
@Data
@ToString
@NoArgsConstructor
public abstract class AbstractColumn {
    protected String columnName;
    private static Gson gson = new Gson();

    /**
     * getData
     *
     * @return
     * @desc 获取数句
     */
    public abstract Object getData();


    public static byte[] parseColumnToBytes(AbstractColumn abstractColumn) {
        return (byte[]) abstractColumn.getData();
    }

    public static String parseColumnToString(AbstractColumn abstractColumn) {
        return gson.toJson(abstractColumn.getData());
    }
    public static int parseColumnToInt(AbstractColumn abstractColumn) {
        return (Integer) abstractColumn.getData();
    }
    public static float parseColumnToFloat(AbstractColumn abstractColumn) {
        return (Float) abstractColumn.getData();
    }
    public static double parseColumnToDouble(AbstractColumn abstractColumn) {
        return (Double) abstractColumn.getData();
    }
    public static BigDecimal parseColumnToDecimal(AbstractColumn abstractColumn) {
        return (BigDecimal) abstractColumn.getData();
    }

}
