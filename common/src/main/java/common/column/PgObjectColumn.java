package common.column;

import org.postgresql.util.PGobject;

import java.io.Serializable;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/1 2:26 下午
 */
public class PgObjectColumn extends AbstractColumn {
    private PGobject data;

    public PgObjectColumn(String columnName, PGobject object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public PGobject getData() {
        return this.data;
    }

}
