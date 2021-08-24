package thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 * @desc sys的线程池
 */
public class SysPoolManager {
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
     * 线程池
     */
    private static ExecutorService executorService;

    static {
        /**
         * sys最少4个线程
         */
        corePoolSize = 4;
        maximumPoolSize = 5;
        executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize + 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(blockSize), new ThreadPoolExecutor.CallerRunsPolicy());
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

    public static void shuntDownNow() {
        executorService.shutdownNow();
    }
}
