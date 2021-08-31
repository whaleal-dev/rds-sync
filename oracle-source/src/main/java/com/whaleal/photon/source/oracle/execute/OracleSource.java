package com.whaleal.photon.source.oracle.execute;

import cache.MemoryCache;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import conf.Configuration;
import configuration.ConfigurationUtil;
import datasource.DBUtil;
import dbconnection.oracle.OracleConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import util.Log;

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
@Slf4j
public class OracleSource extends SourceMetadata {
    //队列
    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();
    //数据库的连接对象
    Connection connection = null;
    JdbcTemplate jdbcTemplate = null;

    public OracleSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceDsName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        connection = OracleConnection.createConnection(DBUtil.getSourceByProcName(proName));
        jdbcTemplate = OracleConnection.getJdbcTemplateBySource(DBUtil.getSourceByProcName(proName));
    }

    @Override
    public void createTask() {
        // 遍历执行源数据源抽取
        // 获取数据源的全部库表
        try {
            getAllDbCollections(sourceName);
        } catch (SQLException e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }
        log.info("获取到所有的库和表");
        // 启动获取提交Task任务的线程
        submitSourceTask();
        log.info("启动获取提交Task任务的线程");
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
        log.info("开始遍历抽取该数据源的所有库表");

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
