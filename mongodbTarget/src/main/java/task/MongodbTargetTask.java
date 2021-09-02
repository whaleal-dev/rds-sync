package task;

import cache.MemoryCache;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import common.taskbase.AbstractTargetTask;
import common.photonV.entity.ProgramInfo;
import dbconnection.mongodb.MongoDbConnection;

import org.bson.Document;
import parse.ParseColumnDataToMongodbData;
import thread.TargetTaskPoolManager;
import util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */
public class MongodbTargetTask extends AbstractTargetTask {

    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 待写入的数据
     */
    private List<WriteModel<Document>> writeModels = new ArrayList<>();
    /**
     * 该target是否停止
     */
    private volatile static Map<String, AtomicBoolean> isStop = new ConcurrentHashMap<>();

    public MongodbTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.mongoClient = MongoDbConnection.getMongoClient(this.targetDsName);
        if (!isStop.containsKey(proName)) {
            synchronized (MongodbTargetTask.class) {
                if (!isStop.containsKey(proName)) {
                    isStop.put(proName, new AtomicBoolean());
                }
            }
        }
    }

    /**
     * 设置某pro的target是否停止
     */
    public static void setIsStopFlagOfTarget(String procName, boolean value) {
        isStop.get(procName).set(value);
    }

    @Override
    public void run() {
        try {
            applyData();
        } finally {
            TargetTaskPoolManager.setTargetActiveThreadNum(proName, -1);
        }

    }


    @Override
    public void applyData() {
        Log.info("启动target任务:" + this.targetDsName);
        while (true) {
            try {
                if (isStop.get(proName).get()) {
                    break;
                }
                BatchDataEntity batchDataEntity = memoryCache.getData();
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的dbTableName
                    this.dbTableName = batchDataEntity.getDbTableName();
                    parseColumnDataToTargetData(batchDataEntity);
                    bulkExecute(dbTableName, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    @Override
    public void parseColumnDataToTargetData(BatchDataEntity batchDataEntity) {
        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            Document document = new Document();
            for (AbstractColumn columnData : columnList) {
                document.append(columnData.getColumnName(), ParseColumnDataToMongodbData.parseColumnData(columnData));
            }
            writeModels.add(new InsertOneModel<>(document));
        }
    }


    @Override
    public void bulkExecute(String dbTable, long batchNo) {
        try {
            if (writeModels.size() == 0) {
                return;
            }
            String dbName = dbTable.split("\\.", 2)[0];
            String tableName = dbTable.split("\\.", 2)[1];
            BulkWriteResult bulkWriteResult = this.mongoClient.getDatabase(dbName).
                    getCollection(tableName).bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(e.getMessage());
        } finally {
            writeModels = new ArrayList<>();
        }
    }

}
