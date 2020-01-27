package com.rebn.tenancy.multi.hibernate;

import com.rebn.tenancy.multi.TenantDataSourceProvider;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;

import javax.sql.DataSource;

/**
 * Title: 多租户数据源提供者
 * Description: 多租户数据源提供者
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class MultiTenantConnectionProviderImpl extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    /**
     * 功能描述 获取默认数据源
     *
     * @return javax.sql.DataSource
     * @author hxs
     * @date 2020/1/27
     */
    @Override
    protected DataSource selectAnyDataSource() {
        return selectDataSource(TenantDataSourceProvider.DEFAULT_KEY);
    }

    /**
     * 功能描述 根据租户号获取数据源
     *
     * @param tenantIdentifier 租户号
     * @return javax.sql.DataSource
     * @author hxs
     * @date 2020/1/27
     */
    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return TenantDataSourceProvider.getTenantDataSource(tenantIdentifier);
    }
} 