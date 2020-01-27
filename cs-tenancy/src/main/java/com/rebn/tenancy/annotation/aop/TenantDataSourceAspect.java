package com.rebn.tenancy.annotation.aop;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.rebn.common.threadpool.ThreadPoolProxyFactory;
import com.rebn.common.util.SpringContextUtil;
import com.rebn.common.util.ThreadTenantUtil;
import com.rebn.tenancy.annotation.TenantDataSource;
import com.rebn.tenancy.multi.TenantDataSourceProvider;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Title: 租户注释处理
 * Description: 租户注释处理
 * Create Time: 2020/1/21
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Component
@Aspect
public class TenantDataSourceAspect {

    private final Logger log = LoggerFactory.getLogger(TenantDataSourceAspect.class);

    /**
     * 定义异步执行部分接口调用需要的线程池对象
     */
    public static ExecutorService pool = ThreadPoolProxyFactory.createThreadPoolExecutor(
            5,
            200,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            new ThreadFactoryBuilder().setNameFormat("task-pool-%d").build(),
            new ThreadPoolExecutor.AbortPolicy());

    @Pointcut("@annotation(com.rebn.tenancy.annotation.TenantDataSource)")
    private void tenantDataSourceAspect() {
    }

    /**
     * 切面执行前
     */
    @Before("tenantDataSourceAspect()")
    public void before(JoinPoint joinPoint) {
        log.debug("the tenant is {} invoking method {} before. ",
                ThreadTenantUtil.getTenant(),
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
    }

    /**
     * 多租户执行
     *
     * @param joinPoint
     * @return 如果有返回值则返回最后一个租户对应的信息
     */
    @Around("tenantDataSourceAspect()")
    public Object around(ProceedingJoinPoint joinPoint) {
        MethodSignature sign = (MethodSignature) joinPoint.getSignature();
        Method method = sign.getMethod();
        TenantDataSource annotation = method.getAnnotation(TenantDataSource.class);
        Set<String> tenantCodes = new HashSet<>(Arrays.asList(annotation.value()));
        if (CollectionUtils.isEmpty(tenantCodes)) {

            // 获取数据源
            tenantCodes = TenantDataSourceProvider.getDataSourceMap().keySet();
        }
        Object result = null;
        if (annotation.async()) {
            log.info("async execute method:{} cannot get result", method);
            if (null != ThreadTenantUtil.getTenant()) {
                result = executeMethod(joinPoint, method, ThreadTenantUtil.getTenant());
            } else {
                for (String tenantCode : tenantCodes) {
                    executeMethodAsync(method, tenantCode);
                }
            }
        } else {
            for (String tenantCode : tenantCodes) {
                result = executeMethod(joinPoint, method, tenantCode);
            }
        }

        if (tenantCodes.size() > 1) {
            log.info("result is the last executed,method:{}, tenantCodes:{}", method, tenantCodes);
        }
        return result;
    }

    /**
     * 异步执行方法
     */
    private void executeMethodAsync(Method method, String tenantCode) {
        try {
            ThreadTenantUtil.setTenant(tenantCode);

            // 执行ioc中class的method
            pool.execute(() ->
                    ReflectionUtils.invokeMethod(method, SpringContextUtil.getBean(method.getDeclaringClass())));
        } catch (Exception e) {
            log.error("invoking method :{},tenantCode:{} Throwable:{}", method.getName(), tenantCode, e);
        } finally {
            log.debug("the tenant is {} invoking method finally. ", ThreadTenantUtil.getTenant());
            ThreadTenantUtil.remove();
        }
    }

    /**
     * 同步执行
     */
    private Object executeMethod(ProceedingJoinPoint joinPoint, Method method, String tenantCode) {
        Object result = null;
        try {
            ThreadTenantUtil.setTenant(tenantCode);
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 防止某个数据源操作异常导致其他数据源无法操作
            log.error("invoking method :{},tenantCode:{} Throwable:{}", method.getName(), tenantCode, e);
        } finally {
            log.debug("the tenant is {} invoking method finally. ", ThreadTenantUtil.getTenant());
            ThreadTenantUtil.remove();
        }
        return result;
    }

    /**
     * 切面执行后
     */
    @After("tenantDataSourceAspect()")
    public void after(JoinPoint joinPoint) {
        log.debug("invoking method {} after.",
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
        if (null != ThreadTenantUtil.getTenant()) {
            ThreadTenantUtil.remove();
        }
    }
}