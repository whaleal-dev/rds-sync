package sourcesplit;


import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import common.dataclass.Range;
import common.dbtype.MongoDbTypeNumber;
import dbconnection.mongodb.MongoDbConnection;
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
    private MongoClient mongoClient;
    /**
     * 数据源名称
     */
    private String sourceDsName;

    public MongodbSourceSplitRange(String sourceDsName) {
        this.sourceDsName = sourceDsName;
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
        Iterator<Map.Entry<String, Integer>> typeIterator = MongoDbTypeNumber.typeNumberMap.entrySet().iterator();
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
                Range range = getMaxAndMinIdByNs(dbTableName, type);
                typeMap.put(type, range);
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
        range.setMaxId(maxId);
        range.setMinId(minId);
        return range;
    }

    /**
     * splitRange 切分数据，每分数据最大长度为50w
     *
     * @param dbTableName 库表名
     * @param rangeOfTable   表范围range
     * @param type           数据类型
     * @return Range  某个区间的range
     * @desc 切分数据，每分数据最大长度为50w
     */
    public Range splitRange(String dbTableName, Range rangeOfTable, int type) {
        Range range = new Range();
        range.set_idType(type);
        String dbName = dbTableName.split("\\.")[0];
        String tableName = dbTableName.split("\\.")[1];
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
            range.setDbTableName(dbTableName);
            rangeOfTable.setMinId(maxIdRTemp);
        } else {
            range.setMinId(rangeOfTable.getMinId());
            range.setMaxId(rangeOfTable.getMaxId());
            range.setDbTableName(dbTableName);
            range.setMax(true);
            //要修改总的rangeOfTable的范围
            rangeOfTable.setMinId(null);
        }
        return range;
    }
}
