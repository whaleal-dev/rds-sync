package thread;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc sys的线程池
 */
public class SysPoolManager extends ThreadPoolManager {

    private static Map<String, SysPoolManager> sysThreadPoolManager = new ConcurrentHashMap<>();

    public SysPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
    }

    public static SysPoolManager getSysTaskPoolManager(String procName) {
        return sysThreadPoolManager.get(procName);
    }

    private static Map<String, AtomicInteger> sysActiveThreadNum = new ConcurrentHashMap<>();
    public static void deleteSysPoolManager(String procName) {
        sysThreadPoolManager.remove(procName);
    }
    public static int setSysActiveThreadNum(String procName, int num) {
        if (!sysActiveThreadNum.containsKey(procName)) {
            synchronized (SysPoolManager.class) {
                if (!sysActiveThreadNum.containsKey(procName)) {
                    sysActiveThreadNum.put(procName, new AtomicInteger(0));
                }
            }
        }
        if (num > 0) {
            sysActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            sysActiveThreadNum.get(procName).decrementAndGet();
        }
        return sysActiveThreadNum.get(procName).get();
    }

    public static void addSysTaskPoolManager(String procName, SysPoolManager sysPoolManager) {
        if (!sysThreadPoolManager.containsKey(procName)) {
            sysThreadPoolManager.put(procName, sysPoolManager);
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
            sysThreadPoolManager.get(procName).executorService.submit(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";

    }

    public static void shuntDownNow(String procName) {
        sysThreadPoolManager.get(procName).executorService.shutdownNow();
        sysThreadPoolManager.get(procName).executorService = null;
        deleteSysPoolManager(procName);
    }
}



