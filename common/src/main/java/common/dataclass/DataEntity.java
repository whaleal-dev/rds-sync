package common.dataclass;


import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
/**
 *  数据实体类抽象类
 * @author jy
 */
public abstract class DataEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 数据
     */
    private String dataa;


}
