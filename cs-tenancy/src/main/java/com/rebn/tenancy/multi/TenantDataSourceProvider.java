package com.rebn.tenancy.multi;

import com.rebn.common.constants.Constants;
import com.rebn.common.entity.DataSourceInfo;
import com.rebn.tenancy.config.properties.HikariProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.DriverDataSource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Title: 租户数据源配置
 * Description: 租户数据源配置
 * Create Time: 2020/1/23
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class TenantDataSourceProvider implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceProvider.class);
    public static final String DEFAULT_KEY = "default";
    public static final String SUFFIX = "_DataSource";
    private static final Map<String, DataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, String> INIT_LIST = new ConcurrentHashMap<>();
    private static final String MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";

    @Autowired
    private Environment env;

    @Autowired
    private HikariProperties hikariProperties;

    @Autowired
    private DefaultListableBeanFactory defaultListableBeanFactory;
    public TenantDataSourceProvider(DataSource dataSource) {
        log.debug("init defaultDataSource.");
        addDefaultDataSource(dataSource);
    }

    /**
     * 根据传进来的tenantCode决定返回的数据源
     */
    public static DataSource getTenantDataSource(String tenantCode) {
        if (StringUtils.isBlank(tenantCode)) {
            log.warn("tenantCode is empty");
            return null;
        }
        if (DATA_SOURCE_MAP.containsKey(tenantCode)) {
            log.info("getTenantDataSource:{}", tenantCode);
            return DATA_SOURCE_MAP.get(tenantCode);
        }
        if (DATA_SOURCE_MAP.isEmpty()) {
            log.warn("defaultDatasource doesn't init, please wait.");
            return null;
        }
        return null;
    }

    /**
     * 校验数据源是否存在
     */
    public static Boolean checkTenantDataSourceExist(String tenantCode) {
        return null != getTenantDataSource(tenantCode);
    }

    /**
     * 获取数据源map
     */
    public static Map<String, DataSource> getDataSourceMap() {
        return DATA_SOURCE_MAP;
    }

    /**
     * 添加数据源
     * HikariDataSource hikariDataSource = new HikariDataSource();
     * hikariDataSource.setDataSource(DataSourceBuilder.create()
     * .url(dataSourceInfo.getUrl().contains(SERVER_TIME_ZONE_KEY) ? dataSourceInfo.getUrl() : dataSourceInfo.getUrl() + SERVER_TIME_ZONE)
     * .username(dataSourceInfo.getUsername())
     * .password(dataSourceInfo.getPassword())
     * .type(HikariDataSource.class)
     * .driverClassName(StringUtils.isNotBlank(dataSourceInfo.getJdbcDriver()) ? dataSourceInfo.getJdbcDriver() : MYSQL_JDBC_DRIVER)
     * .build());
     *
     * @param dataSourceInfo 连接信息
     */
    public void addDataSource(DataSourceInfo dataSourceInfo) {
        log.debug("addDataSource:{}", dataSourceInfo);
        if (null == dataSourceInfo) {
            log.warn("datasource is empty");
            return;
        }
        synchronized (dataSourceInfo.getTenantCode()) {
            if (DATA_SOURCE_MAP.containsKey(dataSourceInfo.getTenantCode())) {
                if (BooleanUtils.isTrue(dataSourceInfo.getNeedOverride())) {
                    removeDataSource(dataSourceInfo.getTenantCode());
                }
                return;
            }
            DataSource hikariDataSource = genDataSource(dataSourceInfo);
            DATA_SOURCE_MAP.put(dataSourceInfo.getTenantCode(), hikariDataSource);
            String beanName = dataSourceInfo.getTenantCode() + SUFFIX;
            this.registerBean(beanName, hikariDataSource);
            DynamicDataSource.getInstance().targetDataSources(DATA_SOURCE_MAP);
        }
    }

    private void registerBean(String beanName, DataSource hikariDataSource) {
        try {
            // 动态注入Bean
            if (defaultListableBeanFactory.containsBean(beanName)) {
                //移除bean的定义和实例
                defaultListableBeanFactory.removeBeanDefinition(beanName);
            }
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(hikariDataSource.getClass());
            beanDefinitionBuilder.setDestroyMethodName("close");
            defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        } catch (Exception e) {
            log.error("registerBean failed", e);
        }
    }

    /**
     * 根据传进来的tenantCode决定返回的数据源
     */
    public DataSource genDataSource(DataSourceInfo dataSourceInfo) {
        log.info("addDataSource:{}", dataSourceInfo);
        Assert.notNull(dataSourceInfo, "tenantInfo is null");
        Assert.hasText(dataSourceInfo.getUrl(), "url is empty");
        Assert.hasText(dataSourceInfo.getUsername(), "username is empty");
        Assert.hasText(dataSourceInfo.getTenantCode(), "tenantCode is empty");
        if (null != dataSourceInfo.getDatabase()) {
            Assert.isTrue(dataSourceInfo.getUrl().contains(dataSourceInfo.getDatabase()), "url is not include multi");
        }
        HikariDataSource defaultHikariDataSource = (HikariDataSource) DATA_SOURCE_MAP.get(DEFAULT_KEY);
        defaultHikariDataSource.getDataSourceProperties();
        HikariConfig hikariConfig = new HikariConfig();
        if (null != hikariProperties.getMaximumPoolSize()) {
            hikariConfig.setMaximumPoolSize(hikariProperties.getMaximumPoolSize());
        } else if (defaultHikariDataSource.getMaximumPoolSize() > 0) {
            hikariConfig.setMaximumPoolSize(defaultHikariDataSource.getMaximumPoolSize());
        }
        if (null != hikariProperties.getMinimumIdle()) {
            hikariConfig.setMinimumIdle(hikariProperties.getMinimumIdle());
        } else if (defaultHikariDataSource.getMinimumIdle() > 0) {
            hikariConfig.setMinimumIdle(defaultHikariDataSource.getMinimumIdle());
        }

        //线下环境 配置
        if (isDevEnv()) {
            hikariConfig.setIdleTimeout(30000);
            hikariConfig.setConnectionTimeout(60000);
            hikariConfig.setValidationTimeout(3000);
            hikariConfig.setMaxLifetime(60000);
        }
        hikariConfig.setJdbcUrl(dataSourceInfo.getUrl());
        hikariConfig.setUsername(dataSourceInfo.getUsername());
        hikariConfig.setPassword(dataSourceInfo.getPassword());
        hikariConfig.setAutoCommit(hikariProperties.getAutoCommit());
        hikariConfig.setDriverClassName(StringUtils.isNotBlank(dataSourceInfo.getJdbcDriver()) ? dataSourceInfo.getJdbcDriver() : MYSQL_JDBC_DRIVER);
        if (null == hikariProperties.getDataSourceProperties()) {
            hikariProperties.setDataSourceProperties(hikariConfig.getDataSourceProperties());
        }
        hikariConfig.setDataSource(new DriverDataSource(
                dataSourceInfo.getUrl(),
                StringUtils.isNotBlank(dataSourceInfo.getJdbcDriver()) ? dataSourceInfo.getJdbcDriver() : MYSQL_JDBC_DRIVER,
                hikariProperties.getDataSourceProperties(),
                dataSourceInfo.getUsername(),
                dataSourceInfo.getPassword()
        ));
        return new HikariDataSource(hikariConfig);
    }

    public void addDefaultDataSource(DataSource dataSource) {
        if (DATA_SOURCE_MAP.containsKey(DEFAULT_KEY)) {
            return;
        }
        DATA_SOURCE_MAP.put(DEFAULT_KEY, dataSource);
        DynamicDataSource.getInstance().setDefaultTargetDataSource(dataSource);
    }

    /**
     * 获取数据库名称
     */
    public static String getDatabase(String tenantCode) {
        HikariDataSource dataSource = (HikariDataSource) getTenantDataSource(tenantCode);
        if (null == dataSource) {
            return null;
        }
        return getDatabaseByUrl(dataSource.getJdbcUrl());
    }

    public static String getDatabaseByUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        try {
            return url.substring(url.lastIndexOf('/') + 1, url.indexOf('?'));
        } catch (Exception e) {
            log.info("getDatabaseByUrl failed");
        }
        return null;
    }

    /**
     * 删除数据源
     */
    public static void removeDataSource(String tenantCode) {
        if (StringUtils.isBlank(tenantCode)) {
            return;
        }
        if (DATA_SOURCE_MAP.containsKey(tenantCode) && !DEFAULT_KEY.equalsIgnoreCase(tenantCode)) {
            ((HikariDataSource) DATA_SOURCE_MAP.get(tenantCode)).close();
            DATA_SOURCE_MAP.remove(tenantCode);
        }
    }

    public static void addInitList(String tenantCode) {
        INIT_LIST.put(tenantCode, tenantCode);
    }

    public static Map<String, String> getInitList() {
        return INIT_LIST;
    }

    public static void removeInitList(String tenantCode) {
        INIT_LIST.remove(tenantCode);
    }

    private boolean isDevEnv() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        return !activeProfiles.contains(Constants.SPRING_PROFILE_PRODUCTION);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

} 