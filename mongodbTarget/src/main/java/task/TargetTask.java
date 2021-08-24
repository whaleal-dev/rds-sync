package task;

import cache.MemoryCache;
import com.mongodb.MongoNamespace;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import lombok.NoArgsConstructor;
import main.MongodbTarget;
import dbconnection.mongodb.MongoDbConnection;
import org.bson.Document;
import util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 写入数据
 */
@NoArgsConstructor
public class TargetTask implements Runnable {

    /**
     * 目标数据源名称
     */
    private String targetDsName;
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * mongoNamespace
     */
    private MongoNamespace mongoNamespace;
    /**
     * 是否停止target线程
     */
    private static volatile boolean isStopFlag = false;
    private List<WriteModel<Document>> writeModels = new ArrayList<>();

    public TargetTask(String targetDsName) {
        this.targetDsName = targetDsName;
        this.mongoClient = MongoDbConnection.getMongoClient(this.targetDsName);
    }

    @Override
    public void run() {
        applyData();
    }

    static AtomicInteger atomicInteger = new AtomicInteger();

    /**
     * applyData 应用数据
     *
     * @desc 应用数据
     */
    public void applyData() {
        Log.info("启动target任务:" + this.targetDsName);
        while (!MongodbTarget.allIsOver) {
            BatchDataEntity batchDataEntity = MemoryCache.getData();
            try {
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的mongoNamespace
                    this.mongoNamespace = new MongoNamespace(batchDataEntity.getDbTableName());
                    System.out.println("target:" + atomicInteger.addAndGet(batchDataEntity.getDataList().size()));
                    // 判断操作行为。如果为INSERTMANY类型，直接应用数据。
                    parseColumnDataToDocument(batchDataEntity);
                    bulkExecute(this.writeModels, "", -1);
                    this.writeModels = new ArrayList<>();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }

    public void parseColumnDataToDocument(BatchDataEntity batchDataEntity) {
        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            Document document = new Document();
            for (AbstractColumn columnData : columnList) {
                document.append(columnData.getColumnName(), columnData.getData());
            }
            writeModels.add(new InsertOneModel<>(document));
        }

    }


    /**
     * bulkExecute 批量写数据
     *
     * @param writeModels 数据集合
     * @desc 批量写数据
     */
    public void bulkExecute(List<WriteModel<Document>> writeModels, String dbTable, long batchNo) {
        try {
            if (writeModels.size() == 0) {
                return;
            }
            String dbName = mongoNamespace.getDatabaseName();
            String tableName = mongoNamespace.getCollectionName();
            BulkWriteResult bulkWriteResult = this.mongoClient.getDatabase(dbName).
                    getCollection(tableName).bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

}
