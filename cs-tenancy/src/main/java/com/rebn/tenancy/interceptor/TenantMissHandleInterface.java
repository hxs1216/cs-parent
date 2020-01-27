package com.rebn.tenancy.interceptor;

import javax.servlet.http.HttpServletRequest;

/**
 * Title: 用于请求缺失租户信息的处理
 * Description: 用于请求缺失租户信息的处理
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public interface TenantMissHandleInterface {

    /**
     * 是否需要处理
     *
     */
    boolean match(HttpServletRequest request);

    /**
     * 生成租户id
     *
     */
    String genTenantCode(HttpServletRequest request);
}
