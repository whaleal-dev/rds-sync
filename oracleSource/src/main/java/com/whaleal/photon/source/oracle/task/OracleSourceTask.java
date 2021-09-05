package com.whaleal.photon.source.oracle.task;

import cache.MemoryCache;
import com.whaleal.photon.source.oracle.parse.OracleDataToColumnData;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.dataclass.Range;
import common.taskbase.AbstractSourceTask;
import common.taskbase.SourceTaskInfo;
import dbconnection.oracle.OracleConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import thread.SourceTaskPoolManager;
import util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * oracle目标源任务
 *
 * @author cs
 * @date 2021/08/31
 */
public class OracleSourceTask extends AbstractSourceTask {

    private static AtomicInteger atomicInteger = new AtomicInteger();
    /**
     * connection
     */
    private Connection connection;

    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();

    public OracleSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        super(taskMetadata, procName, memoryCache, dataBatchSize);
        this.connection = OracleConnection.getConnection(sourceDsName);
    }

    @Override
    public void run() {
        Log.info("启动source任务:" + this.taskMetadata.toString());
        // 读取数据
        getDataFromCollection();
        SourceTaskPoolManager.setSourceActiveThreadNum(procName, -1);
    }

    @Override
    public void getDataFromCollection() {
        String dbTableName = this.taskMetadata.getDbTableName();
        Range range = this.taskMetadata.getRange();
        String query = range.getQuery();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String tableName = dbTableName.split("\\.", 2)[1];
            statement = connection.createStatement();
            String sql = "select * from  " + tableName + " where " + query;
            resultSet = statement.executeQuery(sql);
            Log.info("执行的sql语句" + sql);
            while (resultSet.next()) {
                dataTransformation(resultSet);
                if (cache++ > dataBatchSize) {
                    putDataToCache();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
            if (cache > 0) {
                putDataToCache();
                this.dataList = null;
            }
            Log.info("source任务查询完毕:" + this.taskMetadata.toString());
        }
    }

    /**
     * dataTransformation
     *
     * @param rs
     * @return
     */
    @Override
    public void dataTransformation(Object rs) {
        try {
            //获取有关ResultSet对象中列的类型和属性的信息的对象
            ResultSetMetaData md = ((ResultSet) rs).getMetaData();
            List<AbstractColumn> abstractColumns = new ArrayList<>();
            //获取数据库内容不为空
            //遍历rs中的属性与值
            for (int i = 1; i <= md.getColumnCount(); i++) {
                //属性名下划线改驼峰
                String columnName = md.getColumnName(i);
                //值
                Object values = ((ResultSet) rs).getObject(md.getColumnName(i));
                // System.out.println(columnName +"          "+values.getClass());
                AbstractColumn abstractColumn = OracleDataToColumnData.parseValue(columnName, values);
                abstractColumns.add(abstractColumn);
            }
            this.dataList.add(abstractColumns);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        }

    }

    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        //源数据集合
        batchDataEntity.setDataList(this.dataList);
        //源数据表名
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName() + "BAK");
        //操作行为
        batchDataEntity.setOperation("INSERTMANY");
        //源数据库名
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        //批次号
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        Log.info("sourceNum:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }


}
