package thread;

import java.util.Map;
import java.util.concurrent.*;

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
    public static void addSysTaskPoolManager(String procName, SysPoolManager sysPoolManager) {
        if (!sysThreadPoolManager.containsKey(procName)) {
            sysThreadPoolManager.put(procName, sysPoolManager);
        }
        sysThreadPoolManager.put(procName, sysPoolManager);
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
}



