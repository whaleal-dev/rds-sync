package main;

import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import common.metadata.Metadata;
import common.metadata.SourceTaskMetadata;
import common.dataclass.Range;
import common.thread.SourceTaskPoolManager;
import common.thread.SysPoolManager;
import conf.Configuration;
import dbconnection.mongodb.MongoDbConnection;
import source.Source;
import task.SourceTask;
import util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
public class MongodbSource extends Metadata {

    /**
     * syncModeOfAll
     *
     * @desc 全量任务
     */
    public static void syncModeOfAll() {
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 遍历执行源数据源抽取
        String sourceName = Configuration.sourceName;
        // 获取数据源的全部库表
        getAllDbCollections(sourceName);
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
        isOver = true;
    }


    public static void main(String[] args) {
        syncModeOfAll();
    }


    /**
     * getAllDbCollections 获取数据源中所有的库表名
     *
     * @param sourceName 数据源名称
     * @desc 获取数据源中所有的库表名
     */
    public static void getAllDbCollections(String sourceName) {
        MongoClient mongoClient = MongoDbConnection.getMongoClient(sourceName);
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


    /**
     * startFromSource 把所有库表的中数据进行分片和创造
     *
     * @desc 启动targetTask任务
     */
    public static void startFromSource(String sourceName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceName, new MongoNamespace(next.getValue()));
        }
    }

    /**
     * createSourceEntity 获取这个数据源的某表的且分数据
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public static void createSourceEntity(String sourceName, MongoNamespace mongoNamespace) {
        Source source = new Source(sourceName);
        Map<Integer, Range> map = source.getIdTypes(mongoNamespace);
        Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
        while (rangeMap.hasNext()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Map.Entry<Integer, Range> next = rangeMap.next();
                    Range rangeOfTable = next.getValue();
                    while (rangeOfTable.getMinId() != null) {
                        Range range = source.splitRange(mongoNamespace, rangeOfTable, next.getKey());
                        SourceTaskMetadata taskMetadata = new SourceTaskMetadata(range, mongoNamespace.getFullName(), sourceName);
                        Log.info("taskMetadata配置信息:" + taskMetadata.toString());
                        pushTaskMeta(taskMetadata);
                    }
                }
            };
            SysPoolManager.submit(runnable);
        }
    }

    public static void pushTaskMeta(SourceTaskMetadata taskMetadata) {
        taskMetadataQueue.add(taskMetadata);
    }

    /**
     * submitSourceTask 取task到线程池
     *
     * @desc 获取这个数据源的某表的且分数据
     */
    public static void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (taskMetadataQueue.size() == 0) {
                            TimeUnit.SECONDS.sleep(2);
                        }
                        SourceTaskMetadata taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            System.out.println(taskMetadata.toString());
                            SourceTaskPoolManager.submit(new SourceTask(taskMetadata));
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                    }
                    if(isOver&&taskMetadataQueue.size()==0&&SourceTask.sourceThreadNum.get()==0){
                        SourceTaskPoolManager.shuntDownNow();
                        break;
                    }
                }
            }
        };
        SysPoolManager.submit(runnable);
    }


}
