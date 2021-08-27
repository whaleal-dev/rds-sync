package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Blob;

/**
 * Blob字段类
 *
 * @author: jy
 * @Date: 2021/08/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BlobColumn extends AbstractColumn {

    private Blob data;

    public BlobColumn(String columnName, Blob object) {
        this.columnName = columnName;
        this.data = object;
    }

    @Override
    public Blob getData() {
        return this.data;
    }

}
