package task;

import cache.MemoryCache;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.taskbase.AbstractSourceTask;
import common.taskbase.SourceTaskInfo;
import common.taskbase.SourceTaskInterface;
import dbconnection.mysql.MySqlConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import parse.TransformationMysqlDataToColumn;
import thread.SourceTaskPoolManager;
import util.Log;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: jy
 * @Date: 2021/08/25
 */
public class MysqlSourceTask extends AbstractSourceTask {
    /**
     * connection
     */
    private Connection connection;

    static AtomicInteger atomicInteger = new AtomicInteger();

    public MysqlSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        super(taskMetadata, procName, memoryCache, dataBatchSize);
        this.connection = MySqlConnection.getConnection(this.taskMetadata.getSourceDsName());
    }

    @Override
    public void run() {
        Log.info("启动source任务:" + this.taskMetadata.toString());
        try {
            getDataFromCollection();
        } finally {
            SourceTaskPoolManager.setSourceActiveThreadNum(procName, -1);
        }
    }

    @Override
    public void getDataFromCollection() {
        String sql = this.taskMetadata.getRange().getQuery();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dataTransformation(resultSet);
                System.out.println("dataList    =    " + this.dataList);
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
                    AbstractColumn abstractColumn = TransformationMysqlDataToColumn.parseValue(columnName, values);
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
        //源数据集合
        batchDataEntity.setDataList(this.dataList);
        //源数据表名
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName().split("\\.")[0] + "bak." + this.taskMetadata.getDbTableName().split("\\.")[1]);
        //操作行为
        batchDataEntity.setOperation("INSERTMANY");
        //源数据库名
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        //批次号
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        System.out.println("sourceNum   =   " + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
