package com.rebn.tenancy.config;

import com.rebn.tenancy.multi.TenantDataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

/**
 * Title: TODO
 * Description: TODO
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class TenantConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TenantConfiguration.class);

    @Value("${liquibase.enable:false}")
    private boolean liquibaseEnable;

//    @Autowired
//    @Lazy
//    private TenantDataSourceService tenantDataSourceService;

    @Autowired
    private TenantDataSourceProvider tenantDataSourceProvider;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private LiquibaseProperties liquibaseProperties;

//    @Autowired(required = false)
//    private Set<TenantMissHandleInterface> tenantMissHandleInterfaces;
} 