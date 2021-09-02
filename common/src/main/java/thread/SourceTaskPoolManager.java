package thread;

import util.Log;

import java.util.Map;
import java.util.concurrent.*;
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
    private static Map<String, SourceTaskPoolManager> sourceThreadPoolManager = new ConcurrentHashMap<>();
    /**
     * 某源数据源的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static Map<String, AtomicInteger> sourceActiveThreadNum = new ConcurrentHashMap<>();

    /**
     * 操作某线程池使用数的个数
     */
    public static int setSourceActiveThreadNum(String procName, int num) {
        if (num > 0) {
            sourceActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            sourceActiveThreadNum.get(procName).decrementAndGet();
        }
        return sourceActiveThreadNum.get(procName).get();
    }

    public SourceTaskPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
        sourceActiveThreadNum.put(procName, new AtomicInteger(0));
        // 塞入对象到sourceThreadPoolManager
        sourceThreadPoolManager.put(procName, this);
    }


    /**
     * 删除对象信息
     */
    public static void deleteSourceTaskPoolManager(String procName) {
        sourceThreadPoolManager.remove(procName);
        sourceActiveThreadNum.remove(procName);
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

    /**
     * destroy 销毁数据
     *
     * @param procName
     * @desc 销毁数据
     */
    public static void destroy(String procName) {
        try {
            sourceThreadPoolManager.get(procName).executorService.shutdownNow();
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        deleteSourceTaskPoolManager(procName);
    }
}
