package source;


import com.mongodb.BasicDBObject;
import com.mongodb.MongoNamespace;
import com.mongodb.client.MongoClient;
import common.metadata.SourceTaskMetadata;
import common.TypeNumber;
import common.dataclass.Range;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import dbconnection.mongodb.MongoDbConnection;
import manger.thread.TaskPoolManager;
import org.bson.Document;
import task.SourceTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author: lhp
 * @time: 2021/7/16 3:04 下午
 * @desc:
 */
@NoArgsConstructor
@AllArgsConstructor
public class Source {
    /**
     * mongoClient
     */
    private MongoClient mongoClient;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public Source(String sourceDsName) {
        this.sourceDsName = sourceDsName;
        this.mongoClient = MongoDbConnection.getMongoClient(sourceDsName);
    }

    /**
     * 测试使用
     *
     * @param args
     */
    public static void main(String[] args) {
        Source source = new Source("source.data.mongodb.uri");
        MongoNamespace mongoNamespace = new MongoNamespace("ys", "exam_item_check_log");
        Map<Integer, Range> map = source.getIdTypes(mongoNamespace);
        Iterator<Map.Entry<Integer, Range>> rangeMap = map.entrySet().iterator();
        while (rangeMap.hasNext()) {
            Map.Entry<Integer, Range> next = rangeMap.next();
            Range rangeOfTable = next.getValue();
            while (rangeOfTable.getMinId() != null) {
                Range range = source.splitRange(mongoNamespace, rangeOfTable, next.getKey());
                SourceTaskMetadata taskMetadata = new SourceTaskMetadata(range, mongoNamespace.getFullName(), "source.data.mongodb.uri");
                TaskPoolManager.submit(new SourceTask(taskMetadata));
            }
        }

    }

    /**
     * getIdTypes 获取某表中的主键类型的最大和最小值
     *
     * @param mongoNamespace 库表名
     * @return Map
     * @desc 获取某表中的主键类型的最大和最小值
     */
    public Map getIdTypes(MongoNamespace mongoNamespace) {
        Map<Integer, Range> typeMap = new HashMap<>();
        String dbName = mongoNamespace.getDatabaseName();
        String tableName = mongoNamespace.getCollectionName();
        BasicDBObject basicDBObject = new BasicDBObject();
        Iterator<Map.Entry<String, Integer>> typeIterator = TypeNumber.typeNumberMap.entrySet().iterator();
        while (typeIterator.hasNext()) {
            Map.Entry<String, Integer> next = typeIterator.next();
            int type = next.getValue();
            // 过滤不可能为主键数据的类型
            if (type == 4 || type == 6 || type == 10 || type == 12 || type == -1 || type == 127) {
                continue;
            }
            basicDBObject.append("_id", new Document().append("$type", type));
            Document document = mongoClient.getDatabase(dbName).getCollection(tableName).find(basicDBObject)
                    .projection(new BasicDBObject().append("_id", 1)).first();
            // 判断某类型的主键是否有数据
            if (document != null) {
                Range range = getMaxAndMinIdByNs(mongoNamespace, type);
                typeMap.put(type, range);
            }
        }
        return typeMap;
    }

    /**
     * getMaxAndMinIdByNs 获取某类型主键的最大和最小值
     *
     * @param mongoNamespace 库表名
     * @param type           数据类型
     * @return Range
     * @desc 获取某类型主键的最大和最小值
     */
    public Range getMaxAndMinIdByNs(MongoNamespace mongoNamespace, int type) {
        String dbName = mongoNamespace.getDatabaseName();
        String tableName = mongoNamespace.getCollectionName();
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
        range.setMaxId(maxId);
        range.setMinId(minId);
        return range;
    }

    /**
     * splitRange 切分数据，每分数据最大长度为50w
     *
     * @param mongoNamespace 库表名
     * @param rangeOfTable   表范围range
     * @param type           数据类型
     * @return Range  某个区间的range
     * @desc 切分数据，每分数据最大长度为50w
     */
    public Range splitRange(MongoNamespace mongoNamespace, Range rangeOfTable, int type) {
        Range range = new Range();
        range.set_idType(type);
        String dbName = mongoNamespace.getDatabaseName();
        String tableName = mongoNamespace.getCollectionName();
        BasicDBObject condition = new BasicDBObject();
        // 不要紧在where条件中单独添加type的查询
        condition.append("_id", new Document("$gte", rangeOfTable.getMinId()));
        Document document = mongoClient.getDatabase(dbName).getCollection(tableName).
                find(condition).sort(new BasicDBObject().append("_id", 1)).skip(500000).first();
        // 如果当前minId的后50w条的_id字段为空，说明达到该类型数据的最大值
        if (document != null) {
            Object maxIdRTemp = document.get("_id");
            range.setMinId(rangeOfTable.getMinId());
            range.setMaxId(maxIdRTemp);
            range.setDbTableName(mongoNamespace.getFullName());
            rangeOfTable.setMinId(maxIdRTemp);
        } else {
            range.setMinId(rangeOfTable.getMinId());
            range.setMaxId(rangeOfTable.getMaxId());
            range.setDbTableName(mongoNamespace.getFullName());
            range.setMax(true);
            //要修改总的rangeOfTable的范围
            rangeOfTable.setMinId(null);
        }
        return range;
    }
}
