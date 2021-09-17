package com.whaleal.photon.source.mysql.task;

import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceTask;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.source.mysql.parse.MysqlDataToColumnData;
import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlSourceTask extends AbstractSourceTask {
    /**
     * connection
     */
    private Connection connection;


    public MysqlSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        this.connection = MySqlConnection.getConnection(getProcNameAndBatchNoAndSourceDsName());
    }

    @Override
    public void run() {
        SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 1);
        Log.info("启动source任务:" + this.taskMetadata.toString());
        try {
            getDataFromDbTable();
        } finally {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), -1);
        }
    }

    @Override
    public void getDataFromDbTable() {
        String sql = this.taskMetadata.getRange().getSql().toString();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dataTransformation(resultSet);
                if (cache++ > dataBatchSize) {
                    putDataToCache();
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
     * 获取AbstractColumn集合
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
            if (rs != null) {
                //遍历rs中的属性与值
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    //属性名
                    String columnName = md.getColumnName(i);
                    //值
                    Object values = ((ResultSet) rs).getObject(md.getColumnName(i));
                    AbstractColumn abstractColumn = MysqlDataToColumnData.parseValue(columnName, values);
                    abstractColumns.add(abstractColumn);
                }
                this.dataList.add(abstractColumns);
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }


    /**
     * putDataToCache 推送数据到缓存区中
     *
     * @desc 推送数据到缓存区中
     */
    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        // 源数据集合
        batchDataEntity.setDataList(this.dataList);
        // 源数据表名
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName().split("\\.")[0] + "." + this.taskMetadata.getDbTableName().split("\\.")[1]);
        // 操作行为
        batchDataEntity.setOperation("INSERTMANY");
        // 源数据库名
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        // 批次号
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
