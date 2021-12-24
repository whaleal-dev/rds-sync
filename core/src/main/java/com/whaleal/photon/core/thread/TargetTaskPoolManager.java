package com.whaleal.photon.core.thread;


import com.whaleal.photon.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc 写入目标数据源线程池
 */

public final class TargetTaskPoolManager extends ThreadPoolManager {
    /**
     * 所有target数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static final Map<String, TargetTaskPoolManager> TARGET_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    /**
     * 某target的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static final Map<String, AtomicInteger> TARGET_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    public TargetTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        TARGET_ACTIVE_THREAD_NUM.put(procName, new AtomicInteger(0));
        TARGET_THREAD_POOL_MANAGER.put(procName, this);
    }

    /**
     * 删除对象信息
     */
    public static void deleteTargetTaskPoolManager(String procName) {
        TARGET_THREAD_POOL_MANAGER.remove(procName);
        TARGET_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",target线程池已关闭");
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static int setTargetActiveThreadNum(String procName, int num) {
        if (num > 0) {
            TARGET_ACTIVE_THREAD_NUM.get(procName).incrementAndGet();
        } else if (num < 0) {
            TARGET_ACTIVE_THREAD_NUM.get(procName).decrementAndGet();
        }
        return TARGET_ACTIVE_THREAD_NUM.get(procName).get();
    }


    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static void submit(String procName, Runnable runnable) {
        try {
            TARGET_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交target线程任务时发生错误:" + e.getMessage());
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
            TARGET_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteTargetTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭target线程池时发生错误:" + e.getMessage());
        }

    }
}



