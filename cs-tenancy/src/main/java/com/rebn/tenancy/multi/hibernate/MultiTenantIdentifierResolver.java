package com.rebn.tenancy.multi.hibernate;

import com.rebn.common.util.ThreadTenantUtil;
import com.rebn.tenancy.multi.TenantDataSourceProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.Optional;

/**
 * Title: 多租户解析器
 * Description: 多租户解析器
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class MultiTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    /**
     * 功能描述 获取当前线程租户信息
     *
     * @return java.lang.String
     * @author hxs
     * @date 2020/1/27
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        return Optional.ofNullable(ThreadTenantUtil.getTenant()).orElse(TenantDataSourceProvider.DEFAULT_KEY);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
} 