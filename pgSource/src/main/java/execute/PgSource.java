package execute;

import cache.MemoryCache;
import common.dataclass.Range;
import common.taskbase.SourceTaskInfo;
import common.taskbase.metadata.SourceMetadata;
import common.photonV.entity.ProgramInfo;
import dbconnection.pgserver.PgServerConnection;
import lombok.NoArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import sourcesplit.PgSourceSplitRange;
import task.PgSourceTask;
import thread.SourceTaskPoolManager;
import thread.SysPoolManager;
import util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: PgSource类
 */

public class PgSource extends SourceMetadata {

    private JdbcTemplate jdbcTemplate;

    protected static Map<String, Queue<SourceTaskInfo>> procSourceTask = new ConcurrentHashMap<>();

    public PgSource(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        jdbcTemplate = PgServerConnection.getJdbcTemplate(sourceName);
        procSourceTask.put(proName, taskMetadataQueue);
    }

    @Override
    public void createTask() {
        // 获取数据源的全部库表
        getAllDbCollections(sourceName);
        // 启动获取提交Task任务的线程
        submitSourceTask();
        // 开始遍历抽取该数据源的所有库表
        startFromSource(sourceName, false);
    }

    @Override
    public void getAllDbCollections(String sourceName) {
        String sql = "select  * from information_schema.TABLES where table_type='BASE TABLE' and concat(table_schema,'.',table_name)  ~ ? ";
        List<Map<String, Object>> dbTableMapList = jdbcTemplate.queryForList(sql, dbTableWhite);
        for (Map<String, Object> dbTableNameMap : dbTableMapList) {
            String dbSchemaName = dbTableNameMap.get("table_schema").toString();
            String tableName = dbTableNameMap.get("table_name").toString();
            String dbTable = dbSchemaName + "." + tableName;
            dbTables.put(dbTable, dbTable);
        }
        Log.info("sourceName:" + sourceName + ",全量同步的表列表:" + dbTables);
    }

    @Override
    public void startFromSource(String sourceName, boolean isParallel) {
        Iterator<Map.Entry<String, String>> mapIterator = dbTables.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<String, String> next = mapIterator.next();
            createSourceEntity(sourceName, next.getValue());
            dbTables.remove(next.getKey());
        }
        isGetAllDbTable = true;
    }

    @Override
    public void createSourceEntity(String sourceName, String dbTableName) {
        PgSourceSplitRange source = new PgSourceSplitRange(sourceName);
        List<Range> rangeList = source.getRangeList(dbTableName);
        for (Range range : rangeList) {
            SourceTaskInfo sourceTaskInfo = new SourceTaskInfo();
            sourceTaskInfo.setSourceDsName(sourceName);
            sourceTaskInfo.setDbTableName(dbTableName);
            sourceTaskInfo.setRange(range);
            pushTaskMeta(proName, sourceTaskInfo);
        }
    }

    @Override
    public void submitSourceTask() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        SourceTaskInfo taskMetadata = taskMetadataQueue.poll();
                        if (taskMetadata != null) {
                            SourceTaskPoolManager.setSourceActiveThreadNum(proName, 1);
                            SourceTaskPoolManager.submit(proName, new PgSourceTask(taskMetadata, proName, memoryCache, 128));
                        } else {
                            boolean isOver = taskMetadataQueue.size() == 0 && SourceTaskPoolManager.setSourceActiveThreadNum(proName, 0) == 0 && isGetAllDbTable && dbTables.size() == 0 && SysPoolManager.setSysActiveThreadNum(proName, 0) == 0;
                            if (isOver) {
                                break;
                            }
                            TimeUnit.SECONDS.sleep(2);
                        }
                    } catch (InterruptedException e) {
                        Log.error(e.getMessage());
                        break;
                    }

                }
            }
        };
        SysPoolManager.submit(proName, runnable);
    }

    /**
     * 提交TaskInfo到任务队列中
     *
     * @param procName
     * @param taskMetadata
     */
    public static void pushTaskMeta(String procName, SourceTaskInfo taskMetadata) {
        procSourceTask.get(procName).add(taskMetadata);
    }
}
