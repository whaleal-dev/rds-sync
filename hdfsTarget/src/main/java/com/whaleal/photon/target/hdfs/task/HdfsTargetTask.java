package com.whaleal.photon.target.hdfs.task;

import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.WriteModel;
import com.whaleal.photon.common.cache.MemoryCache;
import com.whaleal.photon.common.common.column.AbstractColumn;
import com.whaleal.photon.common.common.dataclass.BatchDataEntity;
import com.whaleal.photon.common.common.photonV.entity.ProgramInfo;
import com.whaleal.photon.common.common.taskbase.AbstractTargetTask;
import com.whaleal.photon.common.thread.TargetTaskPoolManager;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.hadoop.HadoopConnection;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/22 4:28 下午
 */
public class HdfsTargetTask extends AbstractTargetTask {
    /**
     * fileSystem
     */
    private FileSystem fileSystem;
    /**
     * 待写入的数据
     */
    private List<String> writeJsonList = new ArrayList<>();

    private FSDataOutputStream outputStream;
    private Gson gson = new Gson();

    public HdfsTargetTask(ProgramInfo programInfo, MemoryCache memoryCache) {
        super(programInfo, memoryCache);
        this.fileSystem = HadoopConnection.getHadoopFileSystem(getProcNameAndBatchNoAndTargetDsName());
        try {
            String fileName="hdfs://hadoop1:9000/test/cao2"+System.currentTimeMillis();
            Path file = new Path(fileName+".txt");
            outputStream = fileSystem.create(file,true);

        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            applyData();
            outputStream.close();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            TargetTaskPoolManager.setTargetActiveThreadNum(getProcNameAndBatchNo(), -1);
        }

    }

    @Override
    public void applyData() {
        String proNameAndBatchNo = getProcNameAndBatchNo();
        Log.info("启动target任务:" + proNameAndBatchNo);
        while (true) {
            try {
                if (AbstractTargetTask.getIsStopFlagOfTarget(proNameAndBatchNo)) {
                    break;
                }
                BatchDataEntity batchDataEntity = memoryCache.getData();
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的dbTableName
                    this.dbTableName = batchDataEntity.getDbTableName();
                    parseColumnDataToTargetData(batchDataEntity);
                    bulkExecute(dbTableName, -1);
                } else {
                    //可以进行睡眠
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
            String json = gson.toJson(columnList);
            writeJsonList.add(json);
        }
    }

    @Override
    public void bulkExecute(String dbTable, long batchNo) {
        try {
            if (writeJsonList.size() == 0) {
                return;
            }
//            String[] dbTableArray = dbTable.split("\\.", 2);
//            String dbName = dbTableArray[0];
//            String tableName = dbTableArray[1];
            for (String json : writeJsonList) {
                outputStream.writeUTF(json);
            }
            outputStream.flush();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            writeJsonList = new ArrayList<>();
        }
    }


}
