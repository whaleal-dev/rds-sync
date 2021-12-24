package com.whaleal.photon.common.common.columntype.other;

import java.util.HashMap;
import java.util.Map;

/**
 * @desc: 每个mongodb数据类型对象的int
 * @author: lhp
 * @time: 2021/7/22 10:33 上午
 */
public class MongoDbTypeNumber {
    public static final Map<String, Integer> TYPE_NUMBER_MAP = new HashMap<>();
    static {
        TYPE_NUMBER_MAP.put("double", 1);
        TYPE_NUMBER_MAP.put("string", 2);
        TYPE_NUMBER_MAP.put("object", 3);
        TYPE_NUMBER_MAP.put("array", 4);
        TYPE_NUMBER_MAP.put("binData", 5);
        TYPE_NUMBER_MAP.put("undefined", 6);
        TYPE_NUMBER_MAP.put("objectId", 7);
        TYPE_NUMBER_MAP.put("bool", 8);
        TYPE_NUMBER_MAP.put("date", 9);
        TYPE_NUMBER_MAP.put("null", 10);
        TYPE_NUMBER_MAP.put("regex", 11);
        TYPE_NUMBER_MAP.put("dbPointer", 12);
        TYPE_NUMBER_MAP.put("javascript", 13);
        TYPE_NUMBER_MAP.put("symbol", 14);
        TYPE_NUMBER_MAP.put("javascriptWithScope", 15);
        TYPE_NUMBER_MAP.put("int", 16);
        TYPE_NUMBER_MAP.put("timestamp", 17);
        TYPE_NUMBER_MAP.put("long", 18);
        TYPE_NUMBER_MAP.put("decimal", 19);
        TYPE_NUMBER_MAP.put("minKey", -1);
        TYPE_NUMBER_MAP.put("maxKey", 127);
    }
}
