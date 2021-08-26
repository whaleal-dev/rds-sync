package task;

import cache.MemoryCache;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import common.column.AbstractColumn;
import common.dataclass.BatchDataEntity;
import dbconnection.mongodb.MongoDbConnection;
import lombok.NoArgsConstructor;
import util.Log;
import util.SqlUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: zb
 * @time: 2021/8/24 10:48 上午
 * @desc: 写入数据
 */

@NoArgsConstructor
public class MysqlTargetTask implements Runnable{
    /**
     * 目标数据源名称
     */
    private String targetDsName;
    /**
     * mongoNamespace
     */
    private MongoNamespace mongoNamespace;
    private List<String> writeModels = new ArrayList<String>();

    public MysqlTargetTask(String targetDsName) {
        this.targetDsName = targetDsName;
    }

    @Override
    public void run() {
        applyData();
    }

    public void applyData() {
        Log.info("启动target任务:" + this.targetDsName);
        while (true) {
            BatchDataEntity batchDataEntity = MemoryCache.getData();
            try {
                // 从缓存中获取一批数据
                if (batchDataEntity != null) {
                    // 当前任务拉取的mongoNamespace
                    this.mongoNamespace = new MongoNamespace(batchDataEntity.getDbTableName());
                    parseColumnDataToDocument(batchDataEntity);
                    bulkExecute(this.writeModels);
                    this.writeModels = new ArrayList<String>();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.error(e.getMessage());
            }
        }
    }
    /**
     * parseColumnDataToDocument 数据处理
     *
     * @param batchDataEntity 缓存数据
     * @desc 数据处理
     */
    public void parseColumnDataToDocument(BatchDataEntity batchDataEntity) {
        String dbName = mongoNamespace.getDatabaseName();
        String tableName = mongoNamespace.getCollectionName();
        List<List<AbstractColumn>> dataList = batchDataEntity.getDataList();
        for (List<AbstractColumn> columnList : dataList) {
            //列名和类型Map
            Map<String,String> hashMap = new HashMap<>();
            //字段长度Map
            Map<String, Integer> maxMap = new HashMap<>();
            //字段
            StringBuffer column = new StringBuffer();
            //属性值
            StringBuffer value = new StringBuffer();
            for (AbstractColumn columnData : columnList) {

                String type = columnData.getData().getClass().getSimpleName();
                int length = columnData.getData().toString().length();
                //查询数据库中是否存在这个表
                int num = SqlUtil.isData("select count(*)  from information_schema.TABLES t where t.TABLE_SCHEMA ='"+dbName+"' and t.TABLE_NAME ='"+tableName+"'");
                if(num == 1){
                    //存在，查询表中的结构
                    List<Map<String,Object>> array = new ArrayList<Map<String,Object>>();
                    //查询表中的字段和类型
                    String sql = "select column_name,data_type,CHARACTER_MAXIMUM_LENGTH,NUMERIC_SCALE from information_schema.COLUMNS where table_name = '"+tableName+"' and table_schema = '"+dbName+"'";
                    array = SqlUtil.selectData(sql);
                    for (int i = 0;i < array.size();i++) {
                        if(!hashMap.containsKey(array.get(i).get("column_name"))){
                            hashMap.put(array.get(i).get("column_name").toString(),array.get(i).get("data_type").toString());
                        }
                        if(!maxMap.containsKey(array.get(i).get("column_name"))){
                            hashMap.put(array.get(i).get("column_name").toString(),array.get(i).get("CHARACTER_MAXIMUM_LENGTH").toString());
                        }
                    }
                } else {
                    //不存在，创建表
                    SqlUtil.data("CREATE DATABASE IF NOT EXISTS " + dbName);
                    SqlUtil.data("CREATE Table"+tableName+"("+ columnData.getColumnName() +" " + type + "(" + length + "))");
                    hashMap.put(columnData.getColumnName(),columnData.getData().toString());
                }
                //HashMap中没有这个属性就加入
                if (!hashMap.containsKey(columnData.getColumnName())){
                    hashMap.put(columnData.getColumnName(),columnData.getData().toString());
                    String sql;
                    if (type.equals("String")||type.equals("Boolean")||columnData.getData() instanceof Map||columnData.getData() instanceof Array ){
                        sql = "ALTER TABLE"+tableName+ "ADD" + columnData.getColumnName() +" "+ "varchar("+ length +")";
                    } else if(type.equals("Double")||type.equals("Long")||type.equals("Float")||type.equals("Decimal")){
                        //字符串分割，获取精度
                        String arr[] = columnData.getColumnName().split(".");
                        sql = "ALTER TABLE"+tableName+ "ADD" + columnData.getColumnName() +" " + type + "(" + length + "," + arr[1].length()+")";
                    } else if(columnData.getData() instanceof Date ){
                        sql = "ALTER TABLE"+tableName+ "ADD" + columnData.getColumnName() +" "+type;
                    } else {
                        sql = "ALTER TABLE"+tableName+ "ADD" + columnData.getColumnName() +" " + type +"(" + length + ")";
                    }
                    //表中加入这个字段
                    SqlUtil.data(sql);
                    //maxMap中没有这个属性就加入
                    if (!maxMap.containsKey(columnData.getColumnName())){
                        maxMap.put(columnData.getColumnName(), length);
                    } else {
                        //数据长度不够，修改长度
                        if(length > maxMap.get(columnData.getColumnName())){
                            //加锁，防止数据长度被多个线程修改
                            synchronized (this){
                                if (length > maxMap.get(columnData.getColumnName())){
                                    SqlUtil.data("ALTER TABLE"+tableName+ "MODIFY" + columnData.getColumnName() +" " + type +"(" + length +")");
                                    maxMap.put(columnData.getColumnName(), length);
                                }
                            }
                        }
                    }
                }
                column.append(columnData.getColumnName()).append(",");
                if (type.equals("String")||type.equals("Boolean")||columnData.getData() instanceof Date ||columnData.getData() instanceof Map||columnData.getData() instanceof Array){
                    value.append("'").append(columnData.getData()).append("'").append(",");
                } else {
                    value.append(columnData.getData()).append(",");
                }
            }
            String sql = "(" + column.substring(0,column.length()-1)
                    + ") values (" + value.substring(0,value.length()-1) + ")";
            writeModels.add(sql);
        }

    }


    /**
     * bulkExecute 写数据
     *
     * @param writeModels 数据集合
     * @desc 写数据
     */
    public void bulkExecute(List<String> writeModels) {
        try {
            if (writeModels.size() == 0) {
                return;
            }
            String tableName = mongoNamespace.getCollectionName();
            for (int i = 0;i < writeModels.size();i++) {
                String sql = "insert into"+ tableName + writeModels.get(i);
                SqlUtil.data(sql);
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
    }
}
