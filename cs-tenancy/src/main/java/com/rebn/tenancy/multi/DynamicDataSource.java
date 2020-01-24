package com.rebn.tenancy.multi;

import com.rebn.common.util.ThreadTenantUtil;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Title: 动态数据源
 * Description: Spring提供的轻量级数据源切换方式AbstractRoutingDataSource
 * Create Time: 2020/1/23
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static DynamicDataSource instance;

    private DynamicDataSource() {
    }

    /**
     * 获取实例
     *
     */
    public static synchronized DynamicDataSource getInstance() {
        if (null == instance) {
            instance = new DynamicDataSource();
        }
        return instance;
    }

    /**
     * 设置数据源
     *
     */
    public void targetDataSources(Map<String, DataSource> dataSourceMap) {
        if (null == dataSourceMap) {
            return;
        }
        Map<Object, Object> targetDataSources = new HashMap<>(16);
        dataSourceMap.forEach(targetDataSources::put);
        this.setTargetDataSources(targetDataSources);
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    @Override
    public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return Optional.ofNullable(ThreadTenantUtil.getTenant()).orElse(TenantDataSourceProvider.DEFAULT_KEY);
    }
}