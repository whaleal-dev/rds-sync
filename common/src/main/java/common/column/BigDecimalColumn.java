package common.column;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * BigDecimal字段类
 *
 * @author: jy
 * @Date: 2021/08/26
 */
public class BigDecimalColumn extends AbstractColumn {

    private BigDecimal data;

    public BigDecimalColumn(String columnName, BigDecimal data) {
        this.columnName = columnName;
        this.data = data;
    }

    @Override
    public BigDecimal getData() {
        return this.data;
    }

}
