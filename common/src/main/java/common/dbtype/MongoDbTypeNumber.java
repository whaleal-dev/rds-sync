package common.dbtype;

import java.util.HashMap;
import java.util.Map;

/**
 * @desc: 每个mongodb数据类型对象的int
 * @author: lhp
 * @time: 2021/7/22 10:33 上午
 */
public class MongoDbTypeNumber {
    public static final Map<String, Integer> typeNumberMap = new HashMap<>();

    static {
        typeNumberMap.put("double", 1);
        typeNumberMap.put("string", 2);
        typeNumberMap.put("object", 3);
        typeNumberMap.put("array", 4);
        typeNumberMap.put("binData", 5);
        typeNumberMap.put("undefined", 6);
        typeNumberMap.put("objectId", 7);
        typeNumberMap.put("bool", 8);
        typeNumberMap.put("date", 9);
        typeNumberMap.put("null", 10);
        typeNumberMap.put("regex", 11);
        typeNumberMap.put("dbPointer", 12);
        typeNumberMap.put("javascript", 13);
        typeNumberMap.put("symbol", 14);
        typeNumberMap.put("javascriptWithScope", 15);
        typeNumberMap.put("int", 16);
        typeNumberMap.put("timestamp", 17);
        typeNumberMap.put("long", 18);
        typeNumberMap.put("decimal", 19);
        typeNumberMap.put("minKey", -1);
        typeNumberMap.put("maxKey", 127);
    }
}
