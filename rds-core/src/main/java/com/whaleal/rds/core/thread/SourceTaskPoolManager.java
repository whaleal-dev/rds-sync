package com.whaleal.rds.core.thread;



import com.whaleal.rds.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc 源数据源读取线程池
 */
public class SourceTaskPoolManager extends ThreadPoolManager {

    /**
     * 所有源数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static final Map<String, SourceTaskPoolManager> SOURCE_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    /**
     * 某源数据源的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static final Map<String, AtomicInteger> SOURCE_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    /**
     * 变更某线程池使用数的个数
     */
    public static int setSourceActiveThreadNum(String procName, int num) {
        if (num > 0) {
            SOURCE_ACTIVE_THREAD_NUM.get(procName).incrementAndGet();
        } else if (num < 0) {
            SOURCE_ACTIVE_THREAD_NUM.get(procName).decrementAndGet();
        }
        return SOURCE_ACTIVE_THREAD_NUM.get(procName).get();
    }

    public SourceTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        SOURCE_ACTIVE_THREAD_NUM.put(procName, new AtomicInteger(0));
        // 塞入对象到sourceThreadPoolManager
        SOURCE_THREAD_POOL_MANAGER.put(procName, this);
    }


    /**
     * 删除某程序线程池对象信息
     */
    public static void deleteSourceTaskPoolManager(String procName) {
        SOURCE_THREAD_POOL_MANAGER.remove(procName);
        SOURCE_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",source线程池已关闭");
    }


    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(String procName, Runnable runnable) {
        try {
            SOURCE_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交source线程任务时发生错误:" + e.getMessage());
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }

    /**
     * destroy 销毁数据
     *
     * @param procName
     * @desc 销毁数据
     */
    public static void destroy(String procName) {
        try {
            SOURCE_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteSourceTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭source线程池时发生错误:" + e.getMessage());
        }
    }
}
