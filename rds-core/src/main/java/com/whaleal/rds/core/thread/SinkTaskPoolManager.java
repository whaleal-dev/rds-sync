package com.whaleal.rds.core.thread;


import com.whaleal.rds.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc 写入 Sink 线程池
 */

public final class SinkTaskPoolManager extends ThreadPoolManager {
    /**
     * 所有sink数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static final Map<String, SinkTaskPoolManager> SINK_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    /**
     * 某sink的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static final Map<String, AtomicInteger> SINK_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    public SinkTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        SINK_ACTIVE_THREAD_NUM.put(procName, new AtomicInteger(0));
        SINK_THREAD_POOL_MANAGER.put(procName, this);
    }

    /**
     * 删除对象信息
     */
    public static void deleteSinkTaskPoolManager(String procName) {
        SINK_THREAD_POOL_MANAGER.remove(procName);
        SINK_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",sink线程池已关闭");
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static int setSinkActiveThreadNum(String procName, int num) {
        if (num > 0) {
            SINK_ACTIVE_THREAD_NUM.get(procName).incrementAndGet();
        } else if (num < 0) {
            SINK_ACTIVE_THREAD_NUM.get(procName).decrementAndGet();
        }
        return SINK_ACTIVE_THREAD_NUM.get(procName).get();
    }


    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static void submit(String procName, Runnable runnable) {
        try {
            SINK_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交sink线程任务时发生错误:" + e.getMessage());
        }
    }

    /**
     * 销毁线程池
     *
     * @param procName
     * @desc 销毁线程池
     */
    public static void destroy(String procName) {
        try {
            SINK_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteSinkTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭sink线程池时发生错误:" + e.getMessage());
        }

    }
}



