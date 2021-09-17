package com.whaleal.photon.source.pg.task;


import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.operation.OperationFlag;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractSourceTask;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.core.dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import com.whaleal.photon.source.pg.parse.PgDataToColumnData;
import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 读取表某区间数据
 */
public class PgSourceTask extends AbstractSourceTask {

    /**
     * jdbc
     */
    private JdbcTemplate jdbcTemplate;
    /**
     * connection
     */
    private Connection connection;


    public PgSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        this.jdbcTemplate = PgServerConnection.getJdbcTemplate(getProcNameAndBatchNoAndSourceDsName());
        this.connection = PgServerConnection.getConnection(getProcNameAndBatchNoAndSourceDsName());
    }


    @Override
    public void run() {
        try {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 1);
            Log.info("启动source任务:" + this.taskMetadata.toString());
            // 读取数据
            getDataFromDbTable();
        } finally {
            SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), -1);
            Log.info("关闭source任务:" + this.taskMetadata.toString());
        }
    }


    @Override
    public void getDataFromDbTable() {
//        String dbTableName = this.taskMetadata.getDbTableName();
        Range range = this.taskMetadata.getRange();
//        String query = range.getQuery();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String sql=  range.getSql().toString();;
        try {
            //读取collection中的数据
            connection.setAutoCommit(false);
//            String sql = "select * from  " + dbTableName + " where " + query;
            statement = connection.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            //也可以修改jdbc url通过defaultFetchSize参数来设置，这样默认所以的返回结果都是通过流方式读取.
            statement.setFetchSize(1024);
            resultSet = statement.executeQuery();
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
                    AbstractColumn abstractColumn = PgDataToColumnData.parseValue(columnName, values);
                    abstractColumns.add(abstractColumn);
                }
                this.dataList.add(abstractColumns);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        batchDataEntity.setDataList(this.dataList);
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName());
        batchDataEntity.setOperation(OperationFlag.INSERTMANY);
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
