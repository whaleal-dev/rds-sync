package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;

import com.whaleal.photon.realtime.mongodb.common.AbstractOplogNsBucketTask;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.Document;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: lhp
 * @time: 2021/7/30 11:07 上午
 * @desc: 多个线程操作 此时要处理多个ns。每个ns，最多同时有一个线程处理。
 */
public class OplogNsBucketTask_v3 extends AbstractOplogNsBucketTask {

    public OplogNsBucketTask_v3(OplogMetadata oplogMetadata, String proName) {
        super(oplogMetadata, proName);
    }


    /**
     * parseCreateIndex 解析建立索引
     *
     * @param document oplog数据
     * @desc 解析建立索引  把 CommitIndexBuild转为普通方式建立索引
     */
    @Override
    public void parseCommitIndexBuild(Document document) {
        Document o = (Document) document.get("o");
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        String tableName = o.get("commitIndexBuild").toString();
        List<Document> indexes = document.getList("indexes", Document.class);
        for (Document doc : indexes) {
            // 把commitIndexBuild转为普通方式建立索引
            String indexName = doc.get("name").toString();
            Document newOplogDoc = new Document();
            newOplogDoc.append("ns", document.get("ns"));
            Document newOplogDoc_o = new Document();
            newOplogDoc_o.append("createIndexes", tableName);
            newOplogDoc_o.putAll(doc);
            newOplogDoc.append("o", newOplogDoc_o);
            newOplogDoc.append("ts", document.get("ts"));
            synchronized (oplogMetadata.syncObject) {
                try {
                    oplogMetadata.createOrDropIndexTime = System.currentTimeMillis();
                    if (!oplogMetadata.dbTableIndexIsUserMap.containsKey(dbName + "." + tableName + ".." + indexName)) {
                        oplogMetadata.dbTableIndexIsUserMap.put(dbName + "." + tableName + ".." + indexName, new AtomicBoolean());
                    }
                    oplogMetadata.dbTableIndexMap.put(dbName + "." + tableName + ".." + indexName, newOplogDoc);
                    oplogMetadata.syncObject.notifyAll();
                } catch (Exception ignored) {
                    // 无需处理
                }
            }
        }

    }


    /**
     * parseUpdate 解析更新数据
     *
     * @param document oplog数据
     * @desc 解析更新数据
     */
    @Override
    public void parseUpdate(Document document) {
        String _id = ((Document) document.get("o2")).get("_id").toString();
        int bucketNum = Math.abs(_id.hashCode() % oplogMetadata.getMaxBucketNum());
        // 检查该桶bucketSetMap是否存在。若不存在 则添加
        if (!bucketSetMap.get(bucketNum).add(_id)) {
            putDataToCache(currentDbTable, bucketNum);
            bucketSetMap.get(bucketNum).add(_id);
        }
        // 更新主键
        Document o2 = ((Document) document.get("o2"));
        // 被更新的数据
        Document o = (Document) document.get("o");
        o.remove("$v");
        // 是replace
        if (!o.containsKey("diff")) {
            // 是否开启upsert
            ReplaceOptions option = new ReplaceOptions();
            option.upsert(true);
            bucketWriteModelListMap.get(bucketNum).add(new ReplaceOneModel<Document>(o2, o, option));
        } else {
            Document diff = (Document) o.get("diff");
            // 最终更新的数据
            Document update = new Document();
            // 为更新字段值操作
            Document updateAndInsertValue = new Document();
            if (diff.get("u") != null) {
                Document updateValue = (Document) diff.get("u");
                updateAndInsertValue.putAll(updateValue);
            }
            // 插入新字段信息
            if (diff.get("i") != null) {
                Document insertValue = (Document) diff.get("i");
                updateAndInsertValue.putAll(insertValue);
            }
            // 不需要进行判断有没有i和u
            update.append("$set", updateAndInsertValue);
            // 删除某字段信息
            if (diff.get("d") != null) {
                Document deleteField = (Document) diff.get("d");
                update.append("$unset", deleteField);
            }


            bucketWriteModelListMap.get(bucketNum).add(new UpdateOneModel<Document>(o2, update));
        }
    }
}
