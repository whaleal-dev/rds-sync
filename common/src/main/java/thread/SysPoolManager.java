package thread;

import util.Log;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc sys的线程池
 */
public class SysPoolManager extends ThreadPoolManager {
    /**
     * 所有sys数据源的线程池
     * k为 程序名
     * v为线程池对象
     */
    private static Map<String, SysPoolManager> sysThreadPoolManager = new ConcurrentHashMap<>();
    /**
     * 某源数据源的线程池使用情况
     * k为 程序名
     * v活跃的线程数
     */
    private static Map<String, AtomicInteger> sysActiveThreadNum = new ConcurrentHashMap<>();

//    public static SysPoolManager getSysTaskPoolManager(String procName) {
//        return sysThreadPoolManager.get(procName);
//    }

    public SysPoolManager(String procName, int corePoolSize, int maximumPoolSize) {
        super(procName, corePoolSize, maximumPoolSize);
        sysActiveThreadNum.put(procName, new AtomicInteger(0));
        sysThreadPoolManager.put(procName, this);
    }

    /**
     * 删除对象信息
     */
    public static void deleteSysPoolManager(String procName) {
        sysThreadPoolManager.remove(procName);
        sysActiveThreadNum.remove(procName);
    }

    /**
     * 操作某线程池使用数的个数
     */
    public static int setSysActiveThreadNum(String procName, int num) {
        if (num > 0) {
            sysActiveThreadNum.get(procName).incrementAndGet();
        } else if (num < 0) {
            sysActiveThreadNum.get(procName).decrementAndGet();
        }
        return sysActiveThreadNum.get(procName).get();
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

    /**
     * 销毁线程池
     *
     * @param procName
     * @desc 销毁线程池
     */
    public static void destroy(String procName) {
        try {
            sysThreadPoolManager.get(procName).executorService.shutdownNow();
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        deleteSysPoolManager(procName);
    }
}



