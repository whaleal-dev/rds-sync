package com.whaleal.photon.realtime.mongodb.task;

import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;

import com.whaleal.photon.realtime.mongodb.common.AbstractOplogNsBucketTask;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.Document;

/**
 * @author: lhp
 * @time: 2021/7/30 11:07 上午
 * @desc: 多个线程操作 此时要处理多个ns。每个ns，最多同时有一个线程处理。
 */
public class OplogNsBucketTask_v1 extends AbstractOplogNsBucketTask {


    public OplogNsBucketTask_v1(OplogMetadata oplogMetadata, String proName) {
        super(oplogMetadata, proName);
    }


    @Override
    public void parseCommitIndexBuild(Document document) {
        // 此方法暂未使用 3.2中没有该方法
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
        Document o2 = ((Document) document.get("o2"));
        Document o = (Document) document.get("o");
        o.remove("$v");
        // 有些oplog的o没有$set和$unset
        if (o.get("$set") == null && o.get("$unset") == null) {
            // 是否开启upsert
            ReplaceOptions option = new ReplaceOptions();
            option.upsert(true);
            bucketWriteModelListMap.get(bucketNum).add(new ReplaceOneModel<Document>(o2, o, option));
        } else {
            bucketWriteModelListMap.get(bucketNum).add(new UpdateOneModel<Document>(o2, o));
        }

    }




}
