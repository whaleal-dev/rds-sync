package task;


import cache.MemoryCache;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.dataclass.Range;
import common.taskbase.AbstractSourceTask;
import common.taskbase.SourceTaskInfo;
import dbconnection.pgserver.PgServerConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import parse.PgDataToColumnData;
import thread.SourceTaskPoolManager;
import util.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
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

    public PgSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        super(taskMetadata, procName, memoryCache, dataBatchSize);
        this.jdbcTemplate = PgServerConnection.getJdbcTemplate(this.taskMetadata.getSourceDsName());
        this.connection = PgServerConnection.getConnection(this.taskMetadata.getSourceDsName());
    }


    @Override
    public void run() {
        Log.info("启动source任务:" + this.taskMetadata.toString());
        try {
            // 读取数据
            getDataFromCollection();
        } finally {
            SourceTaskPoolManager.setSourceActiveThreadNum(procName, -1);
        }
    }

    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    @Override
    public void getDataFromCollection() {
        String dbTableName = this.taskMetadata.getDbTableName();
        Range range = this.taskMetadata.getRange();
        String query = range.getQuery();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            //读取collection中的数据
            statement = connection.createStatement();
            String sql = "select * from  " + dbTableName + " where " + query;
            System.out.println("sql=====" + sql);
            resultSet = statement.executeQuery(sql);
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


    static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * putDataToCache 推送数据到缓存区中
     *
     * @desc 推送数据到缓存区中
     */
    @Override
    public void putDataToCache() {
        BatchDataEntity batchDataEntity = new BatchDataEntity();
        batchDataEntity.setDataList(this.dataList);
        batchDataEntity.setDbTableName(this.taskMetadata.getDbTableName().split("\\.")[0] + "bak." + this.taskMetadata.getDbTableName().split("\\.")[1]);
        batchDataEntity.setOperation("INSERTMANY");
        batchDataEntity.setSourceDsName(this.taskMetadata.getSourceDsName());
        batchDataEntity.setBatchNo(System.currentTimeMillis());
        // 推送数据到缓存区中
        memoryCache.putData(batchDataEntity);
        System.out.println("sourceNum:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }
}
