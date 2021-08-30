package com.whaleal.photon.source.oracle.execute;

import cache.MemoryCache;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import conf.Configuration;
import dbconnection.oracle.OracleConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * orcle数据执行器
 *
 * @author cs
 * @date 2021/08/30
 */
public class OracleSource extends SourceMetadata {
    //队列
    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();
    //数据库的连接对象
    Connection connection = null;

    public OracleSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        //connection = OracleConnection.getConnection(configuration.getSourceDsName());
    }

    @Override
    public void createTask() {

    }

    @Override
    public void getAllDbCollections(String sourceName) throws SQLException {

    }

    @Override
    public void startFromSource(String sourceName, boolean isParallel) {

    }

    @Override
    public void createSourceEntity(String sourceName, String dbTableName) {

    }

    @Override
    public void submitSourceTask() {

    }
}
