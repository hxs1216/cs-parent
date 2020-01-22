package com.rebn.common.threadpool;

import java.util.Queue;
import java.util.concurrent.*;

/**
 * Title: 线程池工厂
 * Description: 线程池工厂
 * Create Time: 2020/1/21
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public final class ThreadPoolProxyFactory {

    private static final Queue<Executor> executorList = new ConcurrentLinkedDeque<>();

    private ThreadPoolProxyFactory() {
    }

    public static ExecutorService createThreadPoolExecutor(int corePoolSize,
                                                           int maximumPoolSize,
                                                           long keepAliveTime,
                                                           TimeUnit unit,
                                                           BlockingQueue<Runnable> workQueue,
                                                           ThreadFactory threadFactory,
                                                           RejectedExecutionHandler handler) {
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler
        );
        ExecutorService result = new ExecutorServiceProxy(executorService);
        executorList.add(result);
        return result;
    }

}