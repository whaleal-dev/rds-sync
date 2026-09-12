package com.whaleal.rds.core.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description: 线程池的父类
 * @author: lhp
 * @time: 2021/8/25 2:01 下午
 */
public class ThreadPoolManager {
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
    protected int blockSize = Integer.MAX_VALUE;
    /**
     * 线程池
     */
    protected ExecutorService executorService;

    public ThreadPoolManager(int corePoolSize, int maximumPoolSize) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(blockSize), new ThreadPoolExecutor.CallerRunsPolicy());
    }

}
