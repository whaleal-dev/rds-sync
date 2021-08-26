package thread;

import conf.Configuration;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 */
public class TargetTaskPoolManager extends ThreadPoolManager {

    private static Map<String, TargetTaskPoolManager> targetThreadPoolManager = new ConcurrentHashMap<>();

    public TargetTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
    }

    public static TargetTaskPoolManager getTargetTaskPoolManager(String procName) {
        return targetThreadPoolManager.get(procName);
    }

    public static void addTargetTaskPoolManager(String procName, TargetTaskPoolManager targetTaskPoolManager) {
        if (!targetThreadPoolManager.containsKey(procName)) {
            targetThreadPoolManager.put(procName, targetTaskPoolManager);
        }
        targetThreadPoolManager.put(procName, targetTaskPoolManager);
    }

    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(String procName, Runnable runnable) {
        try {
            targetThreadPoolManager.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";

    }
}



