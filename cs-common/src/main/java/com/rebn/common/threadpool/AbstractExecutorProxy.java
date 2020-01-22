package com.rebn.common.threadpool;

import com.rebn.common.util.ThreadContextUtil;
import com.rebn.common.util.ThreadTenantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Title: 线程池抽象代理
 * Description: spring初始化bean的时候，如果bean实现了InitializingBean接口，会自动调用afterPropertiesSet方法
 * 在bean被销毁的时候如果实现了DisposableBean接口会自动回调destroy方法后然后再销毁
 * Create Time: 2020/1/21
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public abstract class AbstractExecutorProxy implements Executor, InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(AbstractExecutorProxy.class);

    protected abstract Executor getExecutor();

    @Override
    public void execute(Runnable task) {
        this.getExecutor().execute(this.createWrappedRunnable(task));
    }

    protected <T> Callable<T> createCallable(final Callable<T> task) {
        final String currentTenant = ThreadTenantUtil.getTenant();
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        return () -> {
            try {
                ThreadTenantUtil.setTenant(currentTenant);
                ThreadContextUtil.setContext(securityContext);
                return task.call();
            } catch (Exception e) {
                handle(e);
                throw e;
            } finally {
                ThreadTenantUtil.remove();
                ThreadContextUtil.clearContext();
            }
        };
    }

    protected Runnable createWrappedRunnable(final Runnable task) {
        final String currentTenant = ThreadTenantUtil.getTenant();
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        return () -> {
            try {
                ThreadTenantUtil.setTenant(currentTenant);
                ThreadContextUtil.setContext(securityContext);
                task.run();
            } catch (Exception e) {
                handle(e);
                throw e;
            } finally {
                ThreadTenantUtil.remove();
                ThreadContextUtil.clearContext();
            }
        };
    }

    protected void handle(Exception e) {
        log.error("Caught exception", e);
    }
}
