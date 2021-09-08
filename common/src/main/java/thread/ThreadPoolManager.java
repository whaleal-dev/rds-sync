package thread;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 线程池的父类
 * @author: lhp
 * @time: 2021/8/25 2:01 下午
 */
public class ThreadPoolManager {

    /**
     * 程序名称
     */
    protected String procNameAndBatchNo;
    /**
     * 核心线程数
     */
    protected int corePoolSize;
    /**
     * 线程次最大线程数
     */
    protected int maximumPoolSize;
    /**
     * 阻塞的线程数
     */
    protected int blockSize = 1000;
    /**
     * 线程池
     */
    protected ExecutorService executorService;

    public ThreadPoolManager(String procNameAndBatchNo, int corePoolSize, int maximumPoolSize) {
        this.procNameAndBatchNo = procNameAndBatchNo;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(blockSize), new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
