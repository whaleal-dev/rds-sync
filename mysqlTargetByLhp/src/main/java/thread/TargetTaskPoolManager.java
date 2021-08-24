package thread;



import conf.Configuration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 */
public class TargetTaskPoolManager {
    /**
     * 核心线程数
     */
    private static final int corePoolSize;
    /**
     * 线程次最大线程数
     */
    private static final int maximumPoolSize;
    /**
     * 阻塞的线程数
     */
    private static final int blockSize = 100;
    /**
     * 活跃的get线程数
     */
    private static AtomicInteger activeThreadNumOfTarget = new AtomicInteger(0);
    /**
     * 线程池
     */
    private static ExecutorService executorService;

    static {
        corePoolSize = Configuration.targetThreadNum;
        maximumPoolSize = Configuration.targetThreadNum;
        executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(blockSize), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * submit 提交任务
     *
     * @param runnable
     * @desc 提交任务
     */
    public static String submit(Runnable runnable) {
        try {
            executorService.submit(runnable);
        } catch (Exception e) {
            e.printStackTrace();
            return "启动失败:" + e.getMessage();
        }
        return "启动成功";
    }

    /**
     * getActiveTargetThreadNum target线程数
     *
     * @param num
     * @desc 提交任务
     */
    public static synchronized int getActiveTargetThreadNum(int num) {
        if (num > 0) {
            return activeThreadNumOfTarget.incrementAndGet();
        } else if (num < 0) {
            return activeThreadNumOfTarget.decrementAndGet();
        } else {
            return activeThreadNumOfTarget.get();
        }
    }


    public static void shuntDownNow() {
        executorService.shutdownNow();
    }
}
