package common.column;


import com.google.gson.Gson;

/**
 * @description:json字段类 json字符串
 * @author: lhp
 * @time: 2021/8/23 9:55 上午
 */

public class JsonColumn extends AbstractColumn {
    private static Gson gson = new Gson();
    private String data;

    public JsonColumn(String columnName, String object) {
        this.columnName = columnName;
        this.data = object;
    }

//    public JsonColumn(String columnName, Object object) {
//        this.columnName = columnName;
//        this.data = gson.toJson(object);
//    }

    @Override
    public String getData() {
        return this.data;
    }
}
