package com.rebn.common.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ExecutorServiceProxy extends AbstractExecutorProxy implements ExecutorService {

    private final Logger log = LoggerFactory.getLogger(ExecutorServiceProxy.class);

    private final ExecutorService executor;

    public ExecutorServiceProxy(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    protected Executor getExecutor() {
        return this.executor;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(super.createWrappedRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        return executor.invokeAll(tasks.stream().map(super::createCallable).collect(Collectors.toList()));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        return executor.invokeAll(
            tasks.stream().map(super::createCallable).collect(Collectors.toList()),
            timeout,
            unit
        );
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        return executor.invokeAny(tasks.stream().map(super::createCallable).collect(Collectors.toList()));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        return executor.invokeAny(
            tasks.stream().map(super::createCallable).collect(Collectors.toList()),
            timeout,
            unit
        );
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executor.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(super.createCallable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(super.createWrappedRunnable(task), result);
    }

    @Override
    public void destroy() throws Exception {
        this.shutdown();
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

    @Override
    public String toString() {
        return this.executor.toString();
    }
}
