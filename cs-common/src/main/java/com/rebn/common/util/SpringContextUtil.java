package com.rebn.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Title: 上下文公共类
 * Description: 利用Spring后置处理器(BeanPostProcessor)触发该操作；
 * 实现ApplicationContextAware接口, 注入Context到静态变量中
 * Create Time: 2020/1/22
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 获取上下文
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 设置上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 通过名字获取上下文中的bean
     */
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 通过类型获取上下文中的bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return applicationContext.getBean(requiredType);

    }

    /**
     * 获取属性
     */
    public static String getProperty(String key) {
        return applicationContext.getEnvironment().getProperty(key);

    }

    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return applicationContext.getEnvironment().getProperty(key, targetType, defaultValue);
    }


}