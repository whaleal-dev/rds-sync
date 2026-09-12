package com.whaleal.rds.common.cache;


import com.whaleal.rds.common.common.dataclass.BatchDataEntity;
import com.whaleal.rds.common.common.taskbase.AbstractPhotonObject;
import com.whaleal.rds.common.util.Log;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author: lhp
 * @time: 2021/7/20 9:53 上午
 * @desc: 数据缓存类
 */
public class MemoryCache extends AbstractPhotonObject {
    /**
     * 缓存桶个数
     * 默认20个
     */
    private int bucketNum = 20;
    /**
     * 每个缓存桶缓存批次数量
     * 默认20个
     */
    public int cacheBucketSize = 20;
    /**
     * 缓存队列
     */
    private final Queue<BatchDataEntity> batchDataEntityQueue = new ConcurrentLinkedQueue<>();
    /**
     * 缓存类数组
     */
    private final MemoryCache[] memoryCacheList;
    /**
     * 某缓存区是否被使用
     */
    private final AtomicBoolean[] bucketIsUseState;
    /**
     * 空跑次数
     * 可以用来判断读取和写入是否平衡,该功能暂未使用
     */
    public final LongAdder waitTimes = new LongAdder();
    /**
     * 全表桶表的总条数
     */
    public long allDocCount = 0L;

    /**
     * 已写入的总条数
     */
    public final LongAdder writeDocCount = new LongAdder();
    /**
     * init
     *
     * @desc 初始化缓存区类
     */
    public MemoryCache(String taskName, String proName, int bucketNum, int cacheBucketSize, boolean isFirst) {
        super(taskName, proName);
        this.cacheBucketSize = cacheBucketSize;
        this.bucketNum = bucketNum;
        this.memoryCacheList = new MemoryCache[bucketNum];
        this.bucketIsUseState = new AtomicBoolean[bucketNum];
        // isFirst是最开始初始化信息,避免出现较多的对象创建
        for (int i = 0; (i < bucketNum) && isFirst; i++) {
            memoryCacheList[i] = new MemoryCache(taskName, proName, bucketNum, cacheBucketSize, false);
            bucketIsUseState[i] = new AtomicBoolean();
            bucketIsUseState[i].set(false);
        }
    }

    /**
     * getData
     *
     * @return BatchDataEntity
     * @desc 塞入数据
     */
    public BatchDataEntity getData() {
        // 返回的数据
        BatchDataEntity returnValue = null;
        // 没有获取对缓存区的次数。即空跑次数
        int IdlingTimes = 0;
        // 是否继续尝试获取数据
        boolean isWhile = true;
        while (isWhile) {
            // 随机数 范围[0,bucketNum)
            int partition = (int) ((Math.random() * 100) % bucketNum);
            // CAS操作
            boolean pre = bucketIsUseState[partition].get();
            // CAS操作获取缓存区使用权限
            if (!pre && bucketIsUseState[partition].compareAndSet(false, true)) {
                if (!memoryCacheList[partition].batchDataEntityQueue.isEmpty()) {
                    // 设置返回值
                    returnValue = memoryCacheList[partition].batchDataEntityQueue.poll();
                    // 终止循环
                    isWhile = false;
                }
                // 释放'锁'
                bucketIsUseState[partition].set(false);
                IdlingTimes++;
               // 多次未获得数据则返回
                if (IdlingTimes > bucketNum * 2) {
                    break;
                }
            } else if (IdlingTimes++ > bucketNum * 2) {
                // 若没有获取对缓存区的次数大于bucketNum * 2，则进行睡眠1s
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Log.error("程序:" + proName + "从MemoryCache获取数据时发生异常,错误信息:" + e.getMessage());
                }
                waitTimes.increment();
                break;
            }
        }
        return returnValue;
    }

    /**
     * putData
     *
     * @param data
     * @desc 塞入数据
     */
    public void putData(BatchDataEntity data) {
        // 没有获取对缓存区的次数。即空跑次数
        int IdlingTimes = 0;
        // 是否继续尝试获取数据
        boolean isWhile = true;
        while (isWhile) {
            // 随机数 范围[0,bucketNum)
            int partition = (int) ((Math.random() * 100) % bucketNum);
            boolean pre = bucketIsUseState[partition].get();
            // CAS操作获取缓存区使用权限
            if (!pre && bucketIsUseState[partition].compareAndSet(false, true)) {
                // 缓存区是否已满，未满则塞入数据
                if (memoryCacheList[partition].batchDataEntityQueue.size() < cacheBucketSize) {
                    memoryCacheList[partition].batchDataEntityQueue.add(data);
                    // 终止循环
                    isWhile = false;
                }
                // 释放'锁'
                bucketIsUseState[partition].set(false);
                IdlingTimes++;
            }
            // 若没有获取对缓存区的次数大于bucketNum * 2，则进行睡眠1s
            else if (IdlingTimes++ > bucketNum * 2) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Log.error("程序:" + proName + "向MemoryCache放入数据时发生异常,错误信息:" + e.getMessage());
                }
                //设置空跑次数为(bucketNum * 1.8)
                IdlingTimes = (int) (bucketNum * 1.8);
                waitTimes.decrement();
            }
        }
    }

    /**
     * getAllDataBucketNum
     *
     * @desc 获取所有的缓存桶批数据个数
     */
    public int getAllDataBucketNum() {
        // 非原子性操作
        int sum = 0;
        for (int i = 0; i < bucketNum; i++) {
            sum += memoryCacheList[i].batchDataEntityQueue.size();
        }
        return sum;
    }

    /**
     * gcMemoryCache
     *
     * @desc 释放所有缓存数组
     */
    public void gcMemoryCache() {
        for (int i = 0; i < bucketNum; i++) {
            memoryCacheList[i] = null;
            bucketIsUseState[i] = null;
        }
    }
}

