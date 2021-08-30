package task;

import cache.MemoryCache;
import com.google.common.base.CaseFormat;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.taskbase.SourceTaskInfo;
import common.taskbase.SourceTaskInterface;
import conf.DataUtil;
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
public class MysqlSourceTask implements Runnable, SourceTaskInterface {

    private MemoryCache memoryCache;

    /**
     * 程序名
     */
    private String procName;

    /**
     * 任务配置信息
     */
    private SourceTaskInfo taskMetadata;

    /**
     * connection
     */
    private Connection connection;
    /**
     * connection
     */
    private JdbcTemplate jdbcTemplate;

    /**
     * 缓存大小
     */
    private long cache = 0L;

    /**
     * 每个批次数据的大小
     */
    public int dataBatchSize = 128;

    /**
     * 缓存数据集合
     */
    private List<List<AbstractColumn>> dataList = new ArrayList<>();

    public static AtomicInteger sourceThreadNum = new AtomicInteger(0);

    public MysqlSourceTask(SourceTaskInfo taskMetadata, String procName, MemoryCache memoryCache, int dataBatchSize) {
        this.procName = procName;
        this.memoryCache = memoryCache;
        this.dataBatchSize = dataBatchSize;
        this.taskMetadata = taskMetadata;
        this.connection = MySqlConnection.getConnection(this.taskMetadata.getSourceDsName());
        jdbcTemplate = MySqlConnection.getJdbcTemplate(this.taskMetadata.getSourceDsName());
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
        String sql = this.taskMetadata.getRange().getQuery();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                getMysqlAbstractColumn(resultSet);
                System.out.println("dataList    =    " + this.dataList);
                if (cache++ > dataBatchSize) {
                    putDataToCache();
                }

            }
            // 推送最后一批数据
            if (cache > 0) {
                putDataToCache();
                this.dataList = null;
            }
            Log.info("source任务查询完毕:" + this.taskMetadata.toString());
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * 获取AbstractColumn集合
     *
     * @param rs
     * @return
     */
    private void getMysqlAbstractColumn(ResultSet rs) {
        try {
            //获取有关ResultSet对象中列的类型和属性的信息的对象
            ResultSetMetaData md = rs.getMetaData();
            List<AbstractColumn> abstractColumns = new ArrayList<>();
            //获取数据库内容不为空
            if (rs != null) {
                //遍历rs中的属性与值
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    //属性名下划线改驼峰
                    String columnName = md.getColumnName(i);
                    //值
                    Object values = rs.getObject(md.getColumnName(i));
                    AbstractColumn abstractColumn = TransformationMysqlDataToColumn.parseValue(columnName, values);
                    abstractColumns.add(abstractColumn);
                }
                this.dataList.add(abstractColumns);
            }

        } catch (Exception e) {
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
        System.out.println("sourceNum:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }

}
