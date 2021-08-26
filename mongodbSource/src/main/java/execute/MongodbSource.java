package execute;

import cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import common.taskbase.metadata.SourceMetadata;
import common.taskbase.SourceTaskInfo;
import common.dataclass.Range;
import lombok.NoArgsConstructor;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import conf.Configuration;
import dbconnection.mongodb.MongoDbConnection;
import sourcesplit.MongodbSourceSplitRange;
import task.MongodbSourceTask;
import util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
@NoArgsConstructor
public class MongodbSource extends SourceMetadata {
    MongoClient mongoClient = null;

    public MongodbSource(Configuration configuration, MemoryCache memoryCache) {
        this.sourceName = configuration.getSourceName();
        this.taskName = configuration.getTaskName();
        this.proName = configuration.getProName();
        this.dbTableWhite = configuration.getDbTableWhite();
        this.memoryCache = memoryCache;
        procSourceTask.put(proName, taskMetadataQueue);
        mongoClient = MongoDbConnection.getMongoClient(sourceName);
    }

    @Override
    public void createTask() {
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 遍历执行源数据源抽取
        // 获取数据源的全部库表
        getAllDbCollections(sourceName);
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
        isGetAllDbTable = true;
    }

    @Override
    public void getAllDbCollections(String sourceName) {
        MongoIterable<String> mongoIterableOfDb = mongoClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            System.out.println(dbTables);
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") || dbName.equalsIgnoreCase("config")) {
                Log.info("admin,local,config库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = MongoDbConnection.getMongoClient(sourceName).getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                if ((dbTable).matches(dbTableWhite)) {
                    dbTables.put(dbTable, dbTable);
                }
            }
        }
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public void startFromSource(String sourceName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceName, next.getValue());
        }
    }

    @Override
    public void createSourceEntity(String sourceName, String dbTableName) {
        MongodbSourceSplitRange source = new MongodbSourceSplitRange(sourceName);
        Map<Integer, Range> map = source.getIdTypes(dbTableName);
        Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
        while (rangeMap.hasNext()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Map.Entry<Integer, Range> next = rangeMap.next();
                    Range rangeOfTable = next.getValue();
                    while (rangeOfTable.getMinId() != null) {
                        Range range = source.splitRange(dbTableName, rangeOfTable, next.getKey());
                        SourceTaskInfo taskMetadata = new SourceTaskInfo(range, dbTableName, sourceName);
                        // Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                        pushTaskMeta(proName, taskMetadata);
                    }
                }
            };

            SysPoolManager.submit(proName, runnable);
        }
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (taskMetadataQueue.size() == 0) {
                            TimeUnit.SECONDS.sleep(2);
                        }
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.submit(proName, new MongodbSourceTask(taskMetadata, proName, memoryCache, 128));
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                    }
                    if (isGetAllDbTable) {

                    }
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public static void pushTaskMeta(String procName, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }
}
