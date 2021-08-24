package cache;

import common.dataclass.BatchDataEntity;
import conf.Configuration;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author: lhp
 * @time: 2021/7/20 9:53 上午
 * @desc: 数据缓存类
 */
public class MemoryCache {
    /**
     * 缓存区个数
     */
    private static final int cacheNum = Configuration.cacheNum;
    /**
     * 每个缓存区缓存批次数量
     */
    public static final int cacheSize = Configuration.cacheSize;
    /**
     * 缓存队列
     */
    private Queue<BatchDataEntity> batchDataEntityQueue = new ConcurrentLinkedQueue<>();
    /**
     * 缓存类数组
     */
    private static MemoryCache[] cacheList = new MemoryCache[cacheNum];
    /**
     * 某缓存区是否被使用
     */
    private static AtomicBoolean[] isUseState = new AtomicBoolean[cacheNum];
    /**
     * 空跑次数
     */
    public static LongAdder waitTimes = new LongAdder();

    static {
        init();
    }

    /**
     * init
     *
     * @desc 初始化缓存区类
     */
    public static void init() {
        for (int i = 0; i < cacheNum; i++) {
            cacheList[i] = new MemoryCache();
            isUseState[i] = new AtomicBoolean();
            isUseState[i].set(false);
        }
    }

    /**
     * getData
     *
     * @return BatchDataEntity
     * @desc 塞入数据
     */
    public static BatchDataEntity getData() {
        // 返回的数据
        BatchDataEntity returnValue = null;
        // 没有获取对缓存区的次数。即空跑次数
        int IdlingTimes = 0;
        // 是否继续尝试获取数据
        boolean isWhile = true;
        while (isWhile) {
            // 随机数 范围[0,cacheNum)
            int partition = (int) ((Math.random() * 100) % cacheNum);
            // CAS操作
            boolean pre = isUseState[partition].get();
            // CAS操作获取缓存区使用权限
            if (!pre && isUseState[partition].compareAndSet(false, true)) {
                if (!cacheList[partition].batchDataEntityQueue.isEmpty()) {
                    // 设置返回值
                    returnValue = cacheList[partition].batchDataEntityQueue.poll();
                    // 终止循环
                    isWhile = false;
                }
                // 释放'锁'
                isUseState[partition].set(false);
            }
            // 若没有获取对缓存区的次数大于cacheNum * 5，则进行睡眠1s
            else if (IdlingTimes++ > cacheNum * 2) {
                try {
                //    judgePutGetBalance();
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //设置空跑次数为 (cacheNum * 5) - cacheNum
                IdlingTimes = (int)(cacheNum * 1.8);
                waitTimes.increment();
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
    public static void putData(BatchDataEntity data) {
        // 没有获取对缓存区的次数。即空跑次数
        int IdlingTimes = 0;
        // 是否继续尝试获取数据
        boolean isWhile = true;
        while (isWhile) {
            // 随机数 范围[0,cacheNum)
            int partition = (int) ((Math.random() * 100) % cacheNum);
            boolean pre = isUseState[partition].get();
            // CAS操作获取缓存区使用权限
            if (!pre && isUseState[partition].compareAndSet(pre, true)) {
                // 缓存区是否已满，未满则塞入数据
                if (cacheList[partition].batchDataEntityQueue.size() < cacheSize) {
                    cacheList[partition].batchDataEntityQueue.add(data);
                    // 终止循环
                    isWhile = false;
                }
                // 释放'锁'
                isUseState[partition].set(false);
            }
            // 若没有获取对缓存区的次数大于cacheNum * 5，则进行睡眠1s
            else if (IdlingTimes++ > cacheNum * 2) {
                try {
                    //    judgePutGetBalance();
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //设置空跑次数为 (cacheNum * 5) - cacheNum
                IdlingTimes = (int)(cacheNum * 1.8);
                waitTimes.increment();
            }
        }
    }
}
