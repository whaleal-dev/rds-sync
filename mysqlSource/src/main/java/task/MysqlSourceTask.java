package task;

import cache.MemoryCache;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.taskbase.SourceTaskInfo;
import common.taskbase.SourceTaskInterface;
import common.taskbase.metadata.SourceTaskInfo1;
import conf.DataUtil;
import dbconnection.mysql.MySqlConnection;
import util.Log;

import java.sql.Connection;
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
     * 任务配置信息
     */
    private SourceTaskInfo taskMetadata;

    private Connection connection;

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
        this.taskMetadata = taskMetadata;
        this.connection = MySqlConnection.getConnection(procName);
        this.memoryCache = memoryCache;
        this.dataBatchSize = dataBatchSize;
    }

    @Override
    public void run() {
        Log.info("启动source任务:" + this.taskMetadata.toString());
        // 读取数据
        getDataFromCollection();
        sourceThreadNum.addAndGet(-1);
    }

    @Override
    public void getDataFromCollection() {
        //TODO  在这里进行切分数据库id
        String sql = this.taskMetadata.getRangeSql();
        Connection conn = MySqlConnection.getConnection();
        //读取表中的数据
        DataUtil dataUtil = new DataUtil(conn);
        //得到 datalist
        this.dataList = dataUtil.getMysqlDatalist(sql);
        System.out.println("dataList    =    " + this.dataList);
        if (cache++ > dataBatchSize) {
            putDataToCache();
        }
        // 推送最后一批数据
        if (cache > 0) {
            putDataToCache();
            this.dataList = null;
        }
        Log.info("source任务查询完毕:" + this.taskMetadata.toString());
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
        // System.out.println("sourceNum:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
        this.dataList = new ArrayList<>();
        this.cache = 0;
    }

}
