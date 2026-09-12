package com.whaleal.rds.common.util.split;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 范围分开包装
 *
 * @author cs
 * @date 2021/08/27
 */
public class RangeSplitWrap {

    public static List<String> splitAndWrap(String left, String right, int expectSliceNumber,
                                            String columnName, String quote) {
        String[] tempResult = RangeSplitUtil.doAsciiStringSplit(left, right, expectSliceNumber);
        return RangeSplitWrap.wrapRange(tempResult, columnName, quote);
    }

    // warn: do not use this method long->BigInteger
    public static List<String> splitAndWrap(long left, long right, int expectSliceNumber, String columnName) {
        long[] tempResult = RangeSplitUtil.doLongSplit(left, right, expectSliceNumber);
        return RangeSplitWrap.wrapRange(tempResult, columnName);
    }

    public static List<String> splitAndWrap(BigInteger left, BigInteger right, int expectSliceNumber, String columnName) {
        BigInteger[] tempResult = RangeSplitUtil.doBigIntegerSplit(left, right, expectSliceNumber);
        return RangeSplitWrap.wrapRange(tempResult, columnName);
    }

    public static List<String> wrapRange(long[] rangeResult, String columnName) {
        String[] rangeStr = new String[rangeResult.length];
        for (int i = 0, len = rangeResult.length; i < len; i++) {
            rangeStr[i] = String.valueOf(rangeResult[i]);
        }
        return wrapRange(rangeStr, columnName, "");
    }

    public static List<String> wrapRange(BigInteger[] rangeResult, String columnName) {
        String[] rangeStr = new String[rangeResult.length];
        for (int i = 0, len = rangeResult.length; i < len; i++) {
            rangeStr[i] = rangeResult[i].toString();
        }
        return wrapRange(rangeStr, columnName, "");
    }

    public static List<String> wrapRange(String[] rangeResult, String columnName,
                                         String quote) {
        if (null == rangeResult || rangeResult.length < 2) {
            throw new IllegalArgumentException(String.format(
                    "Parameter rangeResult can not be null and its length can not <2. detail:rangeResult=[%s].",
                    StringUtils.join(rangeResult, ",")));
        }

        List<String> result = new ArrayList<String>();

        //TODO  change to  stringbuilder.append(..)
        if (2 == rangeResult.length) {
            result.add(String.format(" (%s%s%s <= %s AND %s <= %s%s%s) ", quote, quoteConstantValue(rangeResult[0]),
                    quote, columnName, columnName, quote, quoteConstantValue(rangeResult[1]), quote));
            return result;
        } else {
            for (int i = 0, len = rangeResult.length - 2; i < len; i++) {
                result.add(String.format(" (%s%s%s <= %s AND %s < %s%s%s) ", quote, quoteConstantValue(rangeResult[i]),
                        quote, columnName, columnName, quote, quoteConstantValue(rangeResult[i + 1]), quote));
            }

            result.add(String.format(" (%s%s%s <= %s AND %s <= %s%s%s) ", quote, quoteConstantValue(rangeResult[rangeResult.length - 2]),
                    quote, columnName, columnName, quote, quoteConstantValue(rangeResult[rangeResult.length - 1]), quote));
            return result;
        }
    }

    public static String wrapFirstLastPoint(String firstPoint, String lastPoint, String columnName,
                                            String quote) {
        return String.format(" ((%s < %s%s%s) OR (%s%s%s < %s)) ", columnName, quote, quoteConstantValue(firstPoint),
                quote, quote, quoteConstantValue(lastPoint), quote, columnName);
    }

    public static String wrapFirstLastPoint(Long firstPoint, Long lastPoint, String columnName) {
        return wrapFirstLastPoint(firstPoint.toString(), lastPoint.toString(), columnName, "");
    }

    public static String wrapFirstLastPoint(BigInteger firstPoint, BigInteger lastPoint, String columnName) {
        return wrapFirstLastPoint(firstPoint.toString(), lastPoint.toString(), columnName, "");
    }

    private static String quoteConstantValue(String aString) {
        return aString.replace("'", "''").replace("\\", "\\\\");
    }
}
