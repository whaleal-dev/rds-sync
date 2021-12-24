package com.whaleal.photon.core.thread;


import com.whaleal.photon.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc 实时线程池
 */

public class RealTimeTaskPoolManager extends ThreadPoolManager {

    public static final String OPLOG_READ = "oplogRead";
    public static final String OPLOG_NS = "oplogNS";
    public static final String OPLOG_NS_BUCKET = "oplogNsBucket";
    public static final String OPLOG_WRITE = "oplogWrite";
    /**
     * 所有实时任务的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static final Map<String, RealTimeTaskPoolManager> REAL_TIME_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    /**
     * 某有实时任务的线程池
     * k为 程序名
     * v活跃的线程数
     */
    private static final Map<String, Map<String, AtomicInteger>> REAL_TIME_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    public RealTimeTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        Map<String, AtomicInteger> map = new ConcurrentHashMap<>();
        map.put(OPLOG_READ, new AtomicInteger());
        map.put(OPLOG_NS, new AtomicInteger());
        map.put(OPLOG_NS_BUCKET, new AtomicInteger());
        map.put(OPLOG_WRITE, new AtomicInteger());
        REAL_TIME_ACTIVE_THREAD_NUM.put(procName, map);
        REAL_TIME_THREAD_POOL_MANAGER.put(procName, this);
    }

    /**
     * 删除对象信息
     */
    public static void deleteRealTimeTaskPoolManager(String procName) {
        REAL_TIME_THREAD_POOL_MANAGER.remove(procName);
        REAL_TIME_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",的实时同步线程池已关闭");
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static String setRealTimeActiveThreadNum(String procName, String type, int num) {
        if (num > 0) {
            REAL_TIME_ACTIVE_THREAD_NUM.get(procName).get(type).incrementAndGet();
        } else if (num < 0) {
            REAL_TIME_ACTIVE_THREAD_NUM.get(procName).get(type).decrementAndGet();
        }
        return REAL_TIME_ACTIVE_THREAD_NUM.get(procName).toString();
    }


    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(String procName, Runnable runnable) {
        try {
            REAL_TIME_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交实时同步线程任务时发生错误:" + e.getMessage());
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }

    /**
     * 销毁线程池
     *
     * @param procName
     * @desc 销毁线程池
     */
    public static void destroy(String procName) {
        try {
            REAL_TIME_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteRealTimeTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭实时同步线程池时发生错误:" + e.getMessage());
        }
    }
}



