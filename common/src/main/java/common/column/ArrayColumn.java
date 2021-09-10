package common.column;

import com.google.gson.Gson;
import util.Log;

import java.util.List;

/**
 * @desc: 数组字段类
 * @author: lhp
 * @time: 2021/8/24 7:37 下午
 */
public class ArrayColumn extends AbstractColumn {
    private static Gson gson = new Gson();
    private Object data;

    public ArrayColumn(String columnName, Object object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Object getData() {
        return this.data;
    }

    @Override
    public String toString() {
        // 防止出现[@的数据。
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringBuilder.append(gson.toJson(this.data));
        } catch (Exception e) {
            Log.error(e.getMessage());
            stringBuilder = new StringBuilder();
//            for (Object object : data) {
//                stringBuilder.append(object.toString());
//            }
        }
        return stringBuilder.toString();
    }
}
