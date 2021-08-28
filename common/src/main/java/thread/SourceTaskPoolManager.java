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

    private static Map<String, AtomicInteger> sourceActiveThreadNum = new ConcurrentHashMap<>();

    public static int setSourceActiveThreadNum(String procName, int num) {
        if (!sourceActiveThreadNum.containsKey(procName)) {
            synchronized (SourceTaskPoolManager.class) {
                if (!sourceActiveThreadNum.containsKey(procName)) {
                    sourceActiveThreadNum.put(procName, new AtomicInteger(0));
                }
            }
        }
        if (num > 0) {
            sourceActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            sourceActiveThreadNum.get(procName).decrementAndGet();
        }
        return sourceActiveThreadNum.get(procName).get();
    }

    public SourceTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
    }

    public static SourceTaskPoolManager getSourceTaskPoolManager(String procName) {
        return sourceThreadPoolManager.get(procName);
    }

    public static void deleteSourceTaskPoolManager(String procName) {
        sourceThreadPoolManager.remove(procName);
    }

    public static void addSourceTaskPoolManager(String procName, SourceTaskPoolManager sourceTaskPoolManager) {
        if (!sourceThreadPoolManager.containsKey(procName)) {
            sourceThreadPoolManager.put(procName, sourceTaskPoolManager);
        }
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

    public static void shuntDownNow(String procName) {
        sourceThreadPoolManager.get(procName).executorService.shutdownNow();
        sourceThreadPoolManager.get(procName).executorService = null;
        deleteSourceTaskPoolManager(procName);
    }
}
