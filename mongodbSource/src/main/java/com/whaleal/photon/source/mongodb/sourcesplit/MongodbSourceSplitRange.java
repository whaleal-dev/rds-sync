package com.whaleal.photon.source.mongodb.sourcesplit;


import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.whaleal.photon.common.common.dataclass.Range;
import com.whaleal.photon.common.common.columntype.other.MongoDbTypeNumber;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.dbconnection.mongodb.MongoDbConnection;
import org.bson.Document;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author: lhp
 * @time: 2021/7/16 3:04 下午
 * @desc:
 */
public class MongodbSourceSplitRange {
    /**
     * mongoClient
     */
    private final MongoClient mongoClient;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    private String proName;

    private int rangeSize = 1000000;

    public MongodbSourceSplitRange(String sourceDsName, String proName) {
        this.sourceDsName = sourceDsName;
        this.proName = proName;
        this.mongoClient = MongoDbConnection.getMongoClient(sourceDsName);
    }

    /**
     * getIdTypes 获取某表中的主键类型的最大和最小值
     *
     * @param dbTableName 库表名
     * @return Map
     * @desc 获取某表中的主键类型的最大和最小值
     */
    public Map getIdTypes(String dbTableName) {
        Map<Integer, Range> typeMap = new HashMap<>();
        String dbName = dbTableName.split("\\.")[0];
        String tableName = dbTableName.split("\\.")[1];
        BasicDBObject basicDBObject = new BasicDBObject();
        Iterator<Map.Entry<String, Integer>> typeIterator = MongoDbTypeNumber.TYPE_NUMBER_MAP.entrySet().iterator();
        while (typeIterator.hasNext()) {
            Map.Entry<String, Integer> next = typeIterator.next();
            int type = next.getValue();
            // 过滤不可能为主键数据的类型
            if (type == 4 || type == 6 || type == 10 || type == 12 || type == -1 || type == 127) {
                continue;
            }
            try {
                basicDBObject.append("_id", new Document().append("$type", type));
                Document document = mongoClient.getDatabase(dbName).getCollection(tableName).find(basicDBObject)
                        .projection(new BasicDBObject().append("_id", 1)).first();
                // 判断某类型的主键是否有数据
                if (document != null) {
                    Range range = getMaxAndMinIdByNs(dbTableName, type);
                    typeMap.put(type, range);
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",切分"+dbTableName+"表任务时发生错误,报错信息:" + e.getMessage());
            }
        }
        return typeMap;
    }

    /**
     * getMaxAndMinIdByNs 获取某类型主键的最大和最小值
     *
     * @param dbTableName 库表名
     * @param type        数据类型
     * @return Range
     * @desc 获取某类型主键的最大和最小值
     */
    public Range getMaxAndMinIdByNs(String dbTableName, int type) {
        String dbName = dbTableName.split("\\.")[0];
        String tableName = dbTableName.split("\\.")[1];
        BasicDBObject condition = new BasicDBObject();
        condition.append("_id", new Document().append("$type", type));
        BasicDBObject sort = new BasicDBObject();
        sort.append("_id", -1);
        Document maxDocument = mongoClient.getDatabase(dbName)
                .getCollection(tableName).find(condition)
                .sort(sort).first();
        sort.append("_id", 1);
        Document minDocument = mongoClient.getDatabase(dbName)
                .getCollection(tableName).find(condition)
                .sort(sort).first();
        Object maxId = maxDocument.get("_id");
        Object minId = minDocument.get("_id");
        // 此某类型数据range的范围
        Range range = new Range();
        range.setMaxValue(maxId);
        range.setMinValue(minId);
        return range;
    }

    /**
     * splitRange 切分数据，每分数据最大长度为50w
     *
     * @param dbTableName  库表名
     * @param rangeOfTable 表范围range
     * @param type         数据类型
     * @return Range  某个区间的range
     * @desc 切分数据，每分数据最大长度为50w
     */
    public Range splitRange(String dbTableName, Range rangeOfTable, int type) {
        Range range = new Range();
        range.setType(type);
        String dbName = dbTableName.split("\\.")[0];
        String tableName = dbTableName.split("\\.")[1];
        BasicDBObject condition = new BasicDBObject();
        // 不要紧在where条件中单独添加type的查询
        condition.append("_id", new Document("$gte", rangeOfTable.getMinValue()));
        Document document = mongoClient.getDatabase(dbName).getCollection(tableName).
                find(condition).sort(new BasicDBObject().append("_id", 1)).skip(500000).first();
        // 如果当前minId的后50w条的_id字段为空，说明达到该类型数据的最大值
        if (document != null) {
            Object maxIdRTemp = document.get("_id");
            range.setMinValue(rangeOfTable.getMinValue());
            range.setMaxValue(maxIdRTemp);
            range.setDbTableName(dbTableName);
            rangeOfTable.setMinValue(maxIdRTemp);
        } else {
            range.setMinValue(rangeOfTable.getMinValue());
            range.setMaxValue(rangeOfTable.getMaxValue());
            range.setDbTableName(dbTableName);
            range.setMax(true);
            //要修改总的rangeOfTable的范围
            rangeOfTable.setMinValue(null);
        }
        return range;
    }


    public void estimateRangeSize(String dbTableName) {
        String dbName = dbTableName.split("\\.")[0];
        String tableName = dbTableName.split("\\.")[1];
        // 100无所谓的,要是没有这个表,也能进行读取的
        long count = 100L;
        try {
            count = mongoClient.getDatabase(dbName).getCollection(tableName).estimatedDocumentCount();
        } catch (Exception e) {
            Log.error("程序:" + proName + ",预估" + dbTableName + "表数据量时发生错误,报错信息:" + e.getMessage());
        }
        if (count > 1000000000) {
            // count大于10亿,每个任务的读取范围为5千w条
            rangeSize = 50000000;
        } else if (count > 100000000) {
            // count大于1亿,每个任务的读取范围为2千w条
            rangeSize = 20000000;
        } else if (count > 10000000) {
            // count大于1千万,每个任务的读取范围为2百w条
            rangeSize = 2000000;
        } else if (count > 1000000) {
            // count大于1百万,每个任务的读取范围为200w条
            rangeSize = 1000000;
        } else {
            // 最小颗粒度为100w每个任务
            rangeSize = 1000000;
        }
    }
}
