package com.rebn.tenancy.annotation;

import java.lang.annotation.*;

/**
 * Title: 租户注释
 * Description: 租户注释
 * Create Time: 2020/1/21
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantDataSource {

    /**
     * 租户名称
     * 默认为空表示所有数据源都会遍历访问
     */
    String[] value() default {};

    /**
     * 默认异步执行
     */
    boolean async() default true;
}