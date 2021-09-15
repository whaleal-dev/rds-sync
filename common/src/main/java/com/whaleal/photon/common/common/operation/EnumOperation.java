package com.whaleal.photon.common.common.operation;

/**
 * @author lhp
 * @time 2021-05-31 13:12:12
 * @desc: 数据类操作行为
 */
public enum EnumOperation {
    /**
     * UPDATE 更新
     */
    UPDATE("UPDATE"),
    /**
     * DELETE 删除
     */
    DELETE("DELETE"),
    /**
     * DELETEMANY 批量删除
     */
    DELETEMANY("DELETEMANY"),
    /**
     * DROPTABLE 删除表
     */
    DROPTABLE("DROPTABLE"),
    /**
     * TRUNCATE 清空表
     */
    TRUNCATE("TRUNCATE"),
    /**
     * INSERT 插入
     */
    INSERT("INSERT"),
    /**
     * ADDCOLUMN 添加字段
     */
    ADDCOLUMN("ADDCOLUMN"),
    /**
     * DROPCOLUMN 删除字段
     */
    DROPCOLUMN("DROPCOLUMN"),
    /**
     * DROPINDEX 删除索引
     */
    DROPINDEX("DROPINDEX"),
    /**
     * RENAMETABLE 表重命名
     */
    RENAMETABLE("RENAMETABLE"),
    /**
     * CREATEINDEX 创建索引
     */
    CREATEINDEX("CREATEINDEX"),
    /**
     * DROPDATABASE 删除库
     */
    DROPDATABASE("DROPDATABASE");

    private String upperCase;

    EnumOperation(String upperCase) {
        this.upperCase = upperCase;
    }

    @Override
    public String toString() {
        return upperCase;
    }
}
