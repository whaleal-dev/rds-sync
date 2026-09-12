package com.whaleal.rds.core.thread;



import com.whaleal.rds.common.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc ddl线程池
 */

public class DDLTaskPoolManager extends ThreadPoolManager {
    /**
     * 所有DDL数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static final Map<String, DDLTaskPoolManager> DDL_THREAD_POOL_MANAGER = new ConcurrentHashMap<>();
    /**
     * 某DDL的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static final Map<String, AtomicInteger> DDL_ACTIVE_THREAD_NUM = new ConcurrentHashMap<>();

    public DDLTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(corePoolSize, maximumPoolSize);
        DDL_ACTIVE_THREAD_NUM.put(procName, new AtomicInteger(0));
        DDL_THREAD_POOL_MANAGER.put(procName, this);
    }

    /**
     * 删除DDL对象信息
     */
    public static void deleteDdlTaskPoolManager(String procName) {
        DDL_THREAD_POOL_MANAGER.remove(procName);
        DDL_ACTIVE_THREAD_NUM.remove(procName);
        Log.info("程序:" + procName + ",DDL线程池已关闭");
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static int setDDLActiveThreadNum(String procName, int num) {
        if (num > 0) {
            DDL_ACTIVE_THREAD_NUM.get(procName).incrementAndGet();
        } else if (num < 0) {
            DDL_ACTIVE_THREAD_NUM.get(procName).decrementAndGet();
        }
        return DDL_ACTIVE_THREAD_NUM.get(procName).get();
    }

    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(String procName, Runnable runnable) {
        try {
            DDL_THREAD_POOL_MANAGER.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",提交DDL线程任务时发生错误:" + e.getMessage());
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
            DDL_THREAD_POOL_MANAGER.get(procName).executorService.shutdownNow();
            deleteDdlTaskPoolManager(procName);
        } catch (Exception e) {
            Log.error("程序:" + procName + ",关闭DDL线程池时发生错误:" + e.getMessage());
        }
    }
}



