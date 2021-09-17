package com.whaleal.photon.common.common.dataclass;


import com.mongodb.BasicDBObject;
import com.whaleal.photon.common.util.StringUtil;
import lombok.*;
import org.bson.Document;

/**
 * @author: lhp
 * @time: 2021/7/19 5:02 下午
 * @desc:数据分片范围
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class Range {
    private String columnName;
    /**
     * 最大值
     */
    private Object maxId;
    /**
     * 最小值
     */
    private Object minId;
    /**
     * 是否为边缘值
     */
    private boolean isMax = false;
    /**
     * 库表名
     */
    private String dbTableName;
    /**
     * 开始时间
     */
    private long startTime;
    /**
     * 结束时间
     */
    private long endTime;
    /**
     * 数据类型
     */
    private int _idType;
    /**
     * 查询语句 一般为sql后面的条件
     */
    private Object query;
    /**
     * 查询语句 一般为一条完整的sql
     */
    private Object sql;

    public Document getQueryForMongodb() {
        if (sql == null) {
            Document match = new Document();
            Document condition = new Document();
            condition.append("_id", new Document("$lt", maxId).append("$gte", minId));
            // 如果是range最大范围，则查询范围是[]。否则[)
            if (isMax) {
                condition.append("_id", new Document("$lte", maxId).append("$gte", minId));
            }
            match.append("$match", condition);
            return match;
        } else {
            Document document = Document.parse(sql.toString());
            return document;
        }
    }
}
