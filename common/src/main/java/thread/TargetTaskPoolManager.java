package thread;

import conf.Configuration;
import util.Log;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 */
public class TargetTaskPoolManager extends ThreadPoolManager {

    private static Map<String, TargetTaskPoolManager> targetThreadPoolManager = new ConcurrentHashMap<>();
    private static Map<String, AtomicInteger> targetActiveThreadNum = new ConcurrentHashMap<>();

    public TargetTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
        Log.info("targetActiveThreadNum" + targetActiveThreadNum.toString());
    }

    public static void deleteSTargetTaskPoolManager(String procName) {
        targetThreadPoolManager.remove(procName);
    }

    public static int setTargetActiveThreadNum(String procName, int num) {
        if (!targetActiveThreadNum.containsKey(procName)) {
            synchronized (TargetTaskPoolManager.class) {
                if (!targetActiveThreadNum.containsKey(procName)) {
                    targetActiveThreadNum.put(procName, new AtomicInteger(0));
                }
            }
        }
        if (num > 0) {
            targetActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            targetActiveThreadNum.get(procName).decrementAndGet();
        }
        return targetActiveThreadNum.get(procName).get();
    }

    public static TargetTaskPoolManager getTargetTaskPoolManager(String procName) {
        return targetThreadPoolManager.get(procName);
    }

    public static void addTargetTaskPoolManager(String procName, TargetTaskPoolManager targetTaskPoolManager) {
        if (!targetThreadPoolManager.containsKey(procName)) {
            targetThreadPoolManager.put(procName, targetTaskPoolManager);
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
            targetThreadPoolManager.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }

    public static void shuntDownNow(String procName) {
        targetThreadPoolManager.get(procName).executorService.shutdownNow();
        targetThreadPoolManager.get(procName).executorService = null;
        deleteSTargetTaskPoolManager(procName);
    }
}



