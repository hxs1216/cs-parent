package com.rebn.tenancy.annotation;

import java.lang.annotation.*;

/**
 * Title: 注解仅用于 TenantMissHandleInterface 实现类标识
 * 是否使用 MyHttpServletRequestWrapper 包装请求
 * Description:
 * Create Time: 2020/1/21
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WrapperRequest {

}
