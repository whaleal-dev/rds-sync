package execute;

import cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import common.taskbase.AbstractSourceExecute;
import common.taskbase.SourceTaskInfo;
import common.dataclass.Range;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import common.photonV.entity.ProgramInfo;
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
 * @desc: MongodbSource类 获取所有的表，切分任务
 */

public class MongodbAbstractSource extends AbstractSourceExecute {
    private MongoClient mongoClient;

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public MongodbAbstractSource(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        procSourceTask.put(procNameAndBatchNo, taskMetadataQueue);
        mongoClient = MongoDbConnection.getMongoClient(procNameAndBatchNoAndSourceDsName);
    }

    @Override
    public void createTask() {
        // 获取数据源的全部库表
        getAllDbCollections(sourceDsName);
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceDsName, false);

    }

    @Override
    public void getAllDbCollections(String sourceName) {
        MongoIterable<String> mongoIterableOfDb = mongoClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") || dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = mongoClient.getDatabase(dbName).listCollectionNames();
            MongoCursor<String> mongoCursorOfTable = mongoIterableOfTable.iterator();
            // 遍历表列表
            while (mongoCursorOfTable.hasNext()) {
                String tableName = mongoCursorOfTable.next();
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(dbTableWhite)) {
                    dbTableNameSet.add(dbTable);
                    dbTables.put(dbTable, dbTable);
                }
            }
        }
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public void startFromSource(String sourceDsName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceDsName, next.getValue());
            dbTables.remove(next.getKey());
        }
        // 所有表均已推送到分析队列中
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceDsName, String dbTableName) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MongodbSourceSplitRange mongodbSourceSplitRange = new MongodbSourceSplitRange(sourceDsName, proName, batchNo);
                Map<Integer, Range> map = mongodbSourceSplitRange.getIdTypes(dbTableName);
                Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
                while (rangeMap.hasNext()) {
                    // sys线程加一
                    SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 1);
                    Map.Entry<Integer, Range> next = rangeMap.next();
                    Range rangeOfTable = next.getValue();
                    while (rangeOfTable.getMinId() != null) {
                        Range range = mongodbSourceSplitRange.splitRange(dbTableName, rangeOfTable, next.getKey());
                        SourceTaskInfo taskMetadata = new SourceTaskInfo(range, dbTableName, sourceDsName);
                        pushTaskMeta(procNameAndBatchNo, taskMetadata);
                    }
                    // sys线程减一
                    SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, -1);
                }
            }
        };
        SysPoolManager.submit(procNameAndBatchNo, runnable);
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {

                            SourceTaskPoolManager.submit(procNameAndBatchNo, new MongodbSourceTask(taskMetadata, proName, memoryCache, 128, batchNo, isUseDeFaultType));
                        } else {
                            boolean isOver = taskMetadataQueue.size() == 0 && SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0) == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0) == 0;
                            if (isOver) {
                                TimeUnit.SECONDS.sleep(10);
                                boolean isOver2 = taskMetadataQueue.size() == 0 && SourceTaskPoolManager.setSourceActiveThreadNum(procNameAndBatchNo, 0) == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(procNameAndBatchNo, 0) == 0;
                                if (isOver2) {
                                    break;
                                }
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.error(e.getMessage());
                        break;
                    }

                }
            }
        };
        SysPoolManager.submit(procNameAndBatchNo, runnable);
    }


    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procNameAndBatchNo).add(taskMetadata);
    }
}
