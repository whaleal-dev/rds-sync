package com.whaleal.rds.source.oracle.task;

import com.whaleal.rds.common.common.operation.OperationFlag;
import com.whaleal.rds.source.oracle.parse.OracleDataToColumnData;
import com.whaleal.rds.common.common.column.AbstractColumn;
import com.whaleal.rds.common.common.dataclass.BatchDataEntity;
import com.whaleal.rds.common.common.dataclass.Range;
import com.whaleal.rds.common.common.syncerV.entity.ProgramInfo;
import com.whaleal.rds.common.common.taskbase.AbstractSourceTask;
import com.whaleal.rds.common.common.taskbase.SourceTaskInfo;
import com.whaleal.rds.core.dbconnection.oracle.OracleConnection;
import com.whaleal.rds.core.thread.SourceTaskPoolManager;
import com.whaleal.rds.common.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * oracle目标源任务
 *
 * @author cs
 * @date 2021/08/31
 */
public class OracleSourceTask extends AbstractSourceTask {

    /**
     * connection
     */
    private Connection connection;

    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();

    public OracleSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        this.connection = OracleConnection.getConnection(getProcNameAndBatchNoAndSourceDsName());
    }

    @Override
    public void run() {
        try {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 1);
            Log.info("启动source任务:" + this.taskMetadata.toString());
            // 读取数据
            getDataFromDbTable();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), -1);
            Log.info("关闭source任务:" + this.taskMetadata.toString());
        }
    }

    @Override
    public void getDataFromDbTable() {
        String dbTableName = this.taskMetadata.getDbTableName();
        Range range = this.taskMetadata.getRange();
       String sql= range.getQuery().toString();;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
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
                AbstractColumn abstractColumn = OracleDataToColumnData.parseValue(columnName, values);
                abstractColumns.add(abstractColumn);
            }
            this.dataList.add(abstractColumns);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }


    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        //源数据集合
        batchDataEntity.setDataList(this.dataList);
        //源数据表名
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName());
        //操作行为
        batchDataEntity.setOperation(OperationFlag.INSERTMANY);
        //源数据库名
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        //批次号
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }


}
