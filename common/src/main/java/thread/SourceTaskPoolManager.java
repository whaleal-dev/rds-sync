package thread;

import conf.Configuration;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 */
public class SourceTaskPoolManager extends ThreadPoolManager {

    private static Map<String, SourceTaskPoolManager> sourceThreadPoolManager = new ConcurrentHashMap<>();

    public SourceTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
    }

    public static SourceTaskPoolManager getSourceTaskPoolManager(String procName) {
        return sourceThreadPoolManager.get(procName);
    }

    public static void addSourceTaskPoolManager(String procName, SourceTaskPoolManager sourceTaskPoolManager) {
        sourceThreadPoolManager.put(procName, sourceTaskPoolManager);
    }

    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(String procName, Runnable runnable) {
        try {
            sourceThreadPoolManager.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }
}
