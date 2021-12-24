package com.whaleal.photon.realtime.mongodb.common;

import org.bson.Document;

/**
 * @description:
 * @author: lhp
 * @time: 2021/11/19 2:03 下午
 */
public interface ParseOplogInterface {
    /**
     * ddl操作
     */
    String DROPTABLE = "drop";
    String CREATETABLE = "create";
    String CREATEINDEX = "createIndexes";
    String DROPINDEX = "dropIndexes";
    String RENAMECOLLECTON = "renameCollection";
    String COMMITINDEXBUILD = "commitIndexBuild";
    String CONVERTOCAPPED = "convertToCapped";
    String DROPDATABASE = "dropDatabase";

    /**
     * parseDropTable 解析删表
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public void parseDropTable(Document document);

    /**
     * parseCreateTable 解析创建表
     *
     * @param document oplog数据
     * @desc 解析创建表
     */
    public void parseCreateTable(Document document);

    /**
     * parseRenameTable 解析表重命名
     *
     * @param document oplog数据
     * @desc 解析表重命名
     */
    public void parseRenameTable(Document document);

    /**
     * parseCreateIndex 解析建立索引
     *
     * @param document oplog数据
     * @desc 解析删表
     */
    public void parseCreateIndex(Document document);

    /**
     * parseDropIndex 解析删除索引
     *
     * @param document oplog数据
     * @desc 解析删除索引
     */
    public void parseDropIndex(Document document);

    /**
     * parseInsert 解析插入数据
     *
     * @param document oplog数据
     * @desc 解析插入数据
     */
    public void parseInsert(Document document);

    /**
     * parseUpdate 解析更新数据
     *
     * @param document oplog数据
     * @desc 解析更新数据
     */
    public void parseUpdate(Document document);

    /**
     * parseDelete 解析删除数据
     *
     * @param document oplog数据
     * @desc 解析删除数据
     */
    public void parseDelete(Document document);

    /**
     * parseDropDatabase 删库
     *
     * @param document oplog数据
     * @desc 删库
     */
    public void parseDropDatabase(Document document);
}
