package com.whaleal.rds.core.thread;


import com.whaleal.rds.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实时（CDC）线程池。
 */
public class RealTimeTaskPoolManager extends ThreadPoolManager {

    public static final String CDC_READ = "cdcRead";
    public static final String CDC_NS = "cdcNs";
    public static final String CDC_NS_BUCKET = "cdcNsBucket";
    public static final String CDC_WRITE = "cdcWrite";

    private static final Map<String, RealTimeTaskPoolManager> REAL_TIME_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, AtomicInteger>> REAL_TIME_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    public RealTimeTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        Map<String, AtomicInteger> map = new ConcurrentHashMap<>();
        map.put(CDC_READ, new AtomicInteger());
        map.put(CDC_NS, new AtomicInteger());
        map.put(CDC_NS_BUCKET, new AtomicInteger());
        map.put(CDC_WRITE, new AtomicInteger());
        REAL_TIME_ACTIVE_THREAD_NUM.put(procName, map);
        REAL_TIME_THREAD_POOL_MANAGER.put(procName, this);
    }

    public static void deleteRealTimeTaskPoolManager(String procName) {
        REAL_TIME_THREAD_POOL_MANAGER.remove(procName);
        REAL_TIME_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",的实时同步线程池已关闭");
    }

    public static String setRealTimeActiveThreadNum(String procName, String type, int num) {
        if (num > 0) {
            REAL_TIME_ACTIVE_THREAD_NUM.get(procName).get(type).incrementAndGet();
        } else if (num < 0) {
            REAL_TIME_ACTIVE_THREAD_NUM.get(procName).get(type).decrementAndGet();
        }
        return REAL_TIME_ACTIVE_THREAD_NUM.get(procName).toString();
    }

    public static String submit(String procName, Runnable runnable) {
        try {
            REAL_TIME_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交实时同步线程任务时发生错误:" + e.getMessage());
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }

    public static void destroy(String procName) {
        try {
            REAL_TIME_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteRealTimeTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭实时同步线程池时发生错误:" + e.getMessage());
        }
    }
}
