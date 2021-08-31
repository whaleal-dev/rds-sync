package common.photonV.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * @author: jy
 * @Date: 2021/08/16
 * @desc 数据源类
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Datasource {

    /**
     * 主键
     */

    private String id;

    /**
     * 数据源名称
     */

    private String name;

    /**
     * 数据库类型
     */

    private String type;

    /**
     * 数据库实例名
     */

    private String dsDatabase;

    /**
     * 数据库schema
     */

    private String dsSchema;

    /**
     * 用户名
     */

    private String username;

    /**
     * 密码
     */

    private String password;

    /**
     * 数据源url
     */

    private String url;

    /**
     * 数据源ip
     */

    private String ip;

    /**
     * 数据源端口
     */

    private String port;

    /**
     * 状态：1启用 2禁用
     */

    private Boolean status;

    /**
     * 创建时间
     */

    private Date createDt;

    /**
     * 更新时间
     */

    private Date updateDt;

    /**
     * 数据源备注
     */

    private String remark;

    /**
     * 数据源更新人
     */

    private String updateUserName;

    /**
     * 数据源选项
     */

    private String dsOption;


    private String jdbcDriverClass;


}
