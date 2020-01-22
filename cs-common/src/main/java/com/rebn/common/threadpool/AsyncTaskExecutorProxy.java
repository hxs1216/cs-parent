package com.rebn.common.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class AsyncTaskExecutorProxy extends AbstractExecutorProxy implements AsyncTaskExecutor {

    private final Logger log = LoggerFactory.getLogger(AsyncTaskExecutorProxy.class);

    private final AsyncTaskExecutor executor;

    public AsyncTaskExecutorProxy(AsyncTaskExecutor executor) {
        this.executor = executor;
    }

    @Override
    protected Executor getExecutor() {
        return this.executor;
    }

    @Override
    public void execute(Runnable task) {

        // 设置租户号+上下文
        executor.execute(super.createWrappedRunnable(task));
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        executor.execute(super.createWrappedRunnable(task), startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(super.createWrappedRunnable(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(super.createCallable(task));
    }

    @Override
    public void destroy() throws Exception {
        if (executor instanceof DisposableBean) {
            DisposableBean bean = (DisposableBean) executor;
            bean.destroy();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (executor instanceof InitializingBean) {
            InitializingBean bean = (InitializingBean) executor;
            bean.afterPropertiesSet();
        }
    }
}
