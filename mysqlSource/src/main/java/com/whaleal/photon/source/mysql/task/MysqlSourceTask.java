package com.whaleal.photon.source.mysql.task;

import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.common.taskbase.AbstractSourceTask;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.core.dbconnection.mysql.MySqlConnection;
import com.whaleal.photon.core.thread.SourceTaskPoolManager;
import com.whaleal.photon.source.mysql.parse.MysqlDataToColumnData;

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
    private final Connection connection;


    public MysqlSourceTask(SourceTaskInfo taskMetadata, ProgramInfo programInfo) {
        super(taskMetadata, programInfo);
        this.connection = MySqlConnection.getConnection(proName);
        SourceTaskPoolManager.setSourceActiveThreadNum(proName, -1);
    }

    @Override
    public void run() {
        // 任务失败后 可以继续进行向下执行
        while (!scanOver) {
            try {
                // 进行判断此程序的状态
                Integer proStatus = ProStatus.getProStatus(proName);
                if (proStatus != ProStatus.FULL_SYNC_RUN) {
                    // 此线程需要停止了
                    if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.FULL_SYNC_STOP) {
                        Log.warn("程序:" + proName + ",检测到该全量程序进入STOP状态,source线程即将关闭");
                        break;
                    } else if (proStatus == ProStatus.FULL_SYNC_SLEEP_AND_REAL_TIME_RUN || proStatus == ProStatus.FULL_SYNC_SLEEP) {
                        // 此线程需要进行睡眠
                        final Object proFullSyncObjectLock = ProStatus.getProFullSyncObjectLock(proName);
                        synchronized (proFullSyncObjectLock) {
                            Log.warn("程序:" + proName + ",检测到该全量程序进入SLEEP状态,source线程即将睡眠");
                            proFullSyncObjectLock.wait();
                        }
                    }
                }
                // 设置taskMetadata的开始时间，后期会使用到该参数
                taskMetadata.setStartTime(System.currentTimeMillis());
                Log.info("程序:" + proName + ",启动source任务:" + this.taskMetadata.toString());
                // 读取数据
                getDataFromDbTable();
            } catch (Exception ignored) {

            } finally {
                // 设置taskMetadata的结束时间，后期会使用到该参数
                taskMetadata.setEndTime(System.currentTimeMillis());
                taskMetadata.getRange().setRangeSize(writeNum);
            }
        }
        // source线程数-1
        SourceTaskPoolManager.setSourceActiveThreadNum(proName, -1);
        long timeDiff = (this.taskMetadata.getEndTime() - this.taskMetadata.getStartTime()) / 1000;
        Log.info("程序:" + proName + ",source任务查询完毕:" + this.taskMetadata.toString() + ",用时" + timeDiff + "S,读取" + writeNum + "条数据");
    }

    @Override
    public void getDataFromDbTable() {
        String sql = this.taskMetadata.getRange().getQuery().toString();
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
        } catch (SQLException e) {
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
