package com.rebn.common.util;

/**
 * Title: 租户线程工具类
 * Description: 租户线程工具类
 * Create Time: 2020/1/21
 *
 * @author hxs
 * @email huangxiaosheng@xxfqc.com
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class ThreadTenantUtil {
    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程对应的租户
     */
    public static void setTenant(String tenantCode) {
        if (null != tenantCode) {
            threadLocal.set(tenantCode);
        }
    }

    /**
     * 获取当前线程对应的租户
     */
    public static String getTenant() {
        return threadLocal.get();
    }

    /**
     * 删除线程变量
     */
    public static void remove() {
        threadLocal.remove();
    }
} 