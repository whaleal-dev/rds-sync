package thread;

import util.Log;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc 写入目标数据源线程池
 */
public class TargetTaskPoolManager extends ThreadPoolManager {
    /**
     * 所有target数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static Map<String, TargetTaskPoolManager> targetThreadPoolManager = new ConcurrentHashMap<>();
    /**
     * 某target的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static Map<String, AtomicInteger> targetActiveThreadNum = new ConcurrentHashMap<>();

    public TargetTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
        targetActiveThreadNum.put(procName, new AtomicInteger(0));
        targetThreadPoolManager.put(procName, this);
    }

    /**
     * 删除对象信息
     */
    public static void deleteSTargetTaskPoolManager(String procName) {
        targetThreadPoolManager.remove(procName);
        targetActiveThreadNum.remove(procName);
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static int setTargetActiveThreadNum(String procName, int num) {
        if (num > 0) {
            targetActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            targetActiveThreadNum.get(procName).decrementAndGet();
        }
        return targetActiveThreadNum.get(procName).get();
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

    /**
     * 销毁线程池
     *
     * @param procName
     * @desc 销毁线程池
     */
    public static void destroy(String procName) {
        try {
            targetThreadPoolManager.get(procName).executorService.shutdownNow();
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        deleteSTargetTaskPoolManager(procName);
    }
}



