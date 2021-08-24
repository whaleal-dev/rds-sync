package common.column;

import org.bson.types.ObjectId;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 11:05 上午
 */
public class ObjectIdColumn extends AbstractColumn {
    private ObjectId data;

    public ObjectIdColumn(String columnName, ObjectId object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public ObjectId getData() {
        return this.data;
    }
}
