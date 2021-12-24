package com.whaleal.photon.source.mongodb.exexute;

import com.whaleal.photon.common.cache.MemoryCache;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.whaleal.photon.common.common.taskbase.AbstractSourceExecute;
import com.whaleal.photon.common.common.taskbase.SourceTaskInfo;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import com.whaleal.photon.core.thread.SourceTaskPoolManager;
import com.whaleal.photon.core.thread.SysPoolManager;
import com.whaleal.photon.source.mongodb.sourcesplit.MongodbSourceSplitRange;
import com.whaleal.photon.source.mongodb.task.MongodbSourceTask;
import com.whaleal.photon.common.util.Log;

import java.util.Iterator;
import java.util.Map;
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
    private final MongoClient mongoClient;

    public MongodbSourceExecute(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        mongoClient = MongoDbConnection.getMongoClient(programInfo.getSourceDsName());
    }

    @Override
    public void start() {
        // 获取数据源的全部库表
        getAllDbTables();
        // 开始删除已经存在的目标表
        if (programInfo.isAutoDropExistDbTable()) {
            // 删表的速度挺快的,不需要进行异步操作
            // CommonTask.dropDbTable(proName,dbTableNameSet, programInfo.getTargetDsName());
        }
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        splitDbTable();
    }

    @Override
    public void getAllDbTables() {
        MongoIterable<String> mongoIterableOfDb = mongoClient.listDatabaseNames();
        // 遍历库列表
        for (String dbName : mongoIterableOfDb) {
            // 此操作有可能无权限遍历库表信息
            if (dbName.equalsIgnoreCase("admin") ||
                    dbName.equalsIgnoreCase("local") ||
                    dbName.equalsIgnoreCase("config")) {
                Log.info(dbName + "库数据不进行同步");
                continue;
            }
            MongoIterable<String> mongoIterableOfTable = mongoClient.getDatabase(dbName).listCollectionNames();
            // 遍历表列表
            for (String tableName : mongoIterableOfTable) {
                String dbTable = dbName + "." + tableName;
                // 顺序不可写法反
                if (dbTable.matches(dbTableWhite) && !dbTable.matches("\\w+\\.system\\..+")) {
                    // 不同步 表名开头为:system.的表
                    dbTableNameSet.add(dbTable);
                    // 计算当前表的数量
                    long documentCount = mongoClient.getDatabase(dbName).getCollection(tableName).estimatedDocumentCount();
                    // 更新总数量
                    memoryCache.allDocCount += documentCount;
                }
            }
        }
        Log.info("程序:" + proName + ",全量同步的表列表:" + dbTableNameSet);
    }

    @Override
    public void splitDbTable() {
        Iterator<String> iterator = dbTableNameSet.iterator();
        while (iterator.hasNext()) {
            String dbTableName = iterator.next();
            createSourceTask(dbTableName);
            iterator.remove();
        }
        // 所有表均已推送到分析队列中
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceTask(final String dbTableName) {
        String proName = programInfo.getProName();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MongodbSourceSplitRange mongodbSourceSplitRange = new MongodbSourceSplitRange(sourceDsName, proName);
                    // 设置每个线程读取的范围大小
                    mongodbSourceSplitRange.estimateRangeSize(dbTableName);
                    Map<Integer, Range> map = mongodbSourceSplitRange.getIdTypes(dbTableName);
                    for (Map.Entry<Integer, Range> next : map.entrySet()) {
                        Range rangeOfTable = next.getValue();
                        while (rangeOfTable.getMinValue() != null) {
                            Range range = mongodbSourceSplitRange.splitRange(dbTableName, rangeOfTable, next.getKey());
                            SourceTaskInfo taskMetadata = new SourceTaskInfo(range, dbTableName, sourceDsName);
                            // 此次同步涉及的任务数++
                            maxSourceTaskInfoNum.incrementAndGet();
                            taskMetadataQueue.add(taskMetadata);
                        }
                    }
                } catch (Exception e) {
                    Log.error("程序:" + MongodbSourceExecute.this.proName + ",切分SourceTask时发生错误,报错信息:" + e.getMessage());
                } finally {
                    // sys线程减一
                    SysPoolManager.setSysActiveThreadNum(proName, -1);
                }
            }
        };
        // sys线程加一
        SysPoolManager.setSysActiveThreadNum(proName, 1);
        // 提交此切分任务
        SysPoolManager.submit(proName, runnable);
    }


    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // 此线程不需要停止 让它一直打乱SourceTaskInfo的顺序吧
                while (true) {
                    try {
                        if (SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0) > programInfo.getSourceThreadNum() * 2) {
                            // 乱序操作
                            if (!programInfo.isParallelSynchronizationMultipleTables()) {
                                // 让SourceTaskInfo乱序 。相邻的两个交换,然后再加入队列中
                                SourceTaskInfo taskMetadataTemp1 = taskMetadataQueue.poll();
                                SourceTaskInfo taskMetadataTemp2 = taskMetadataQueue.poll();
                                // 判断两个是否同时是否为空
                                if (taskMetadataTemp1 != null && taskMetadataTemp2 != null) {
                                    taskMetadataQueue.add(taskMetadataTemp2);
                                    taskMetadataQueue.add(taskMetadataTemp1);
                                } else {
                                    // 数据再塞回去
                                    if (taskMetadataTemp1 != null) {
                                        taskMetadataQueue.add(taskMetadataTemp1);
                                    }
                                    if (taskMetadataTemp2 != null) {
                                        taskMetadataQueue.add(taskMetadataTemp2);
                                    }
                                }
                            }
                            // 就是要睡眠了
                            TimeUnit.SECONDS.sleep(2);
                            continue;
                        }
                        // 拉取任务信息
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            //把任务提交到SourceTaskPoolManager
                            SourceTaskPoolManager.submit(proName, new MongodbSourceTask(taskMetadata, programInfo));
                        } else {
                            int sourceActiveThreadNum = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
                            int sysActiveThreadNum = SysPoolManager.setSysActiveThreadNum(proName, 0);
                            /**
                             *   剩余任务队列==0
                             *   活跃的source线程数==0
                             *   活跃的sys线程数==0
                             *   isGetAllDbTable是否已经获取到全部的表=true
                             *   以上条件作为关闭此线程的条件
                             */
                            boolean isOver = taskMetadataQueue.size() == 0 && sourceActiveThreadNum == 0
                                    && isGetAllDbTable && sysActiveThreadNum == 0;
                            if (isOver) {
                                TimeUnit.SECONDS.sleep(10);
                                // dcl检查
                                int sourceActiveThreadNum2 = SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0);
                                int sysActiveThreadNum2 = SysPoolManager.setSysActiveThreadNum(proName, 0);
                                boolean isOver2 = taskMetadataQueue.size() == 0 && sourceActiveThreadNum2 == 0
                                        && isGetAllDbTable && sysActiveThreadNum2 == 0;
                                if (isOver2) {
                                    break;
                                }
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }
                        // 就是要睡眠了
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        Log.error("程序:" + proName + ",创建SourceTask时发生错误,报错信息:" + e.getMessage());
                        // 出现强制关闭InterruptedException的情况下，关闭此线程
                        break;
                    }
                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    @Override
    public void executeQueryTask() {

    }

}
