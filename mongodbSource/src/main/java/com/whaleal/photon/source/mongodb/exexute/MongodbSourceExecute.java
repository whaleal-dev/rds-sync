package com.whaleal.photon.source.mongodb.exexute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.thread.SourceTaskPoolManager;
import com.whaleal.photon.common.thread.SysPoolManager;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.source.mongodb.sourcesplit.MongodbSourceSplitRange;
import com.whaleal.photon.source.mongodb.task.MongodbSourceTask;
import com.whaleal.photon.common.util.Log;

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

public class MongodbSourceExecute extends AbstractSourceExecute {
    /**
     * mongoClient链接端
     */
    private MongoClient mongoClient;
    /**
     * SourceTaskInfo队列Map
     * k为程序名+批次号
     * v为SourceTaskInfo队列
     */
    protected static Map<String, Queue<SourceTaskInfo>> proSourceTask = new ConcurrentHashMap<>();

    public MongodbSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        proSourceTask.put(getProcNameAndBatchNo(), taskMetadataQueue);
        mongoClient = MongoDbConnection.getMongoClient(getProcNameAndBatchNoAndSourceDsName());
    }

    @Override
    public void start() {
        // 获取数据源的全部库表
        getAllDbTables();
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        splitDbTable();
    }

    @Override
    public void getAllDbTables() {
        MongoIterable<String> mongoIterableOfDb = mongoClient.listDatabaseNames();
        MongoCursor<String> mongoCursorOfDb = mongoIterableOfDb.iterator();
        // 遍历库列表
        while (mongoCursorOfDb.hasNext()) {
            String dbName = mongoCursorOfDb.next();
            if (dbName.equalsIgnoreCase("admin") ||
                    dbName.equalsIgnoreCase("local") ||
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
                    dbTableNameMap.put(dbTable, dbTable);
                }
            }
        }
        Log.info(getProcNameAndBatchNo() + ",全量同步的表列表:" + dbTableNameMap);
    }

    @Override
    public void splitDbTable() {
        Iterator<Map.Entry<String, String>> mapIterator = dbTableNameMap.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceTask(next.getValue());
            dbTableNameMap.remove(next.getKey());
        }
        // 所有表均已推送到分析队列中
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceTask(String dbTableName) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MongodbSourceSplitRange mongodbSourceSplitRange = new MongodbSourceSplitRange(sourceDsName, proName, batchNo);
                Map<Integer, Range> map = mongodbSourceSplitRange.getIdTypes(dbTableName);
                Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
                // sys线程加一
                SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 1);
                while (rangeMap.hasNext()) {
                    Map.Entry<Integer, Range> next = rangeMap.next();
                    Range rangeOfTable = next.getValue();
                    while (rangeOfTable.getMinId() != null) {
                        Range range = mongodbSourceSplitRange.splitRange(dbTableName, rangeOfTable, next.getKey());
                        SourceTaskInfo taskMetadata = new SourceTaskInfo(range, dbTableName, sourceDsName);
                        pushTaskMeta(getProcNameAndBatchNo(), taskMetadata);
                    }
                }
                // sys线程减一
                SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), -1);
            }
        };
        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);
    }


    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0) > 10) {
                            TimeUnit.SECONDS.sleep(10);
                        }
                        TimeUnit.SECONDS.sleep(2);
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.submit(getProcNameAndBatchNo(), new MongodbSourceTask(taskMetadata, programInfo));
                        } else {
                            int sourceActiveThreadNum = SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0);
                            int sysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 0);
                            /**
                             *   剩余任务队列==0
                             *   活跃的source线程数==0
                             *   活跃的sys线程数==0
                             *   isGetAllDbTable是否已经获取到全部的表=true
                             *   dbTableNameMap全部的表是否都进行了切表操作
                             *   以上条件作为关闭此线程的条件
                             */
                            boolean isOver = taskMetadataQueue.size() == 0 && sourceActiveThreadNum == 0
                                    && isGetAllDbTable && dbTableNameMap.size() == 0 && sysActiveThreadNum == 0;
                            if (isOver) {
                                TimeUnit.SECONDS.sleep(10);
                                //dcl检查
                                int sourceActiveThreadNum2 = SourceTaskPoolManager.setSourceActiveThreadNum(getProcNameAndBatchNo(), 0);
                                int sysActiveThreadNum2 = SysPoolManager.setSysActiveThreadNum(getProcNameAndBatchNo(), 0);
                                boolean isOver2 = taskMetadataQueue.size() == 0 && sourceActiveThreadNum2 == 0
                                        && isGetAllDbTable && dbTableNameMap.size() == 0 && sysActiveThreadNum2 == 0;
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
        SysPoolManager.submit(getProcNameAndBatchNo(), runnable);
    }

    public static void pushTaskMeta(String procNameAndBatchNo, SourceTaskInfo sourceTaskInfo) {
        proSourceTask.get(procNameAndBatchNo).add(sourceTaskInfo);
    }
}
