package common.thread;

import conf.Configuration;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author liheping
 * @create 2021/7/7 11:21 上午
 */
public class SourceTaskPoolManager {
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
     * 活跃的source线程数
     */
    private static AtomicInteger activeThreadNumOfSource = new AtomicInteger(0);
    /**
     * 线程池
     */
    private static ExecutorService executorService;

    static {
        corePoolSize = Configuration.sourceThreadNum;
        maximumPoolSize = Configuration.sourceThreadNum;
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
     * getActiveSourceThreadNum source线程数
     *
     * @param num
     * @desc 提交任务
     */
    public static synchronized int getActiveSourceThreadNum(int num) {
        if (num > 0) {
            return activeThreadNumOfSource.incrementAndGet();
        } else if (num < 0) {
            return activeThreadNumOfSource.decrementAndGet();
        } else {
            return activeThreadNumOfSource.get();
        }
    }


    public static void shuntDownNow() {
        executorService.shutdownNow();
    }
}
