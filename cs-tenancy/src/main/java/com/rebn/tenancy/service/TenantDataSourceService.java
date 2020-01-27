package com.rebn.tenancy.service;

import com.rebn.tenancy.config.properties.LocalDataSourceProperties;
import com.rebn.tenancy.config.properties.TenantConfigProperties;
import com.rebn.tenancy.multi.TenantDataSourceProvider;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Title: 租户数据源service
 * Description: 租户数据源service
 * Create Time: 2020/1/27
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Service
@EnableScheduling
public class TenantDataSourceService {

    @Value("${spring.datasource.charset:}")
    private String charset;

    @Autowired
    @Qualifier("tenantLiquibase")
    private SpringLiquibase tenantLiquibase;

    @Autowired
    private TenantDataSourceProvider tenantDataSourceProvider;

    @Autowired(required = false)
    private MongoProperties mongoProperties;


    @Autowired(required = false)
    private LiquibaseProperties liquibaseProperties;

    /**
     * 本地配置的数据源 需要与默认数据源在同一个库下
     */
    @Autowired
    private LocalDataSourceProperties localDataSourceProperties;

    @Value("${eureka.instance.instanceId}")
    private String instanceId;

    private final String APP_NAME;
    private final String BASE_URL;
    private final RestTemplate REST_TEMPLATE;

    public TenantDataSourceService(TenantConfigProperties tenantConfigProperties) {
        this.APP_NAME = tenantConfigProperties.getAppName();
        this.BASE_URL = tenantConfigProperties.getUaaBaseUrl();
        this.REST_TEMPLATE = tenantConfigProperties.getUaaRestTemplate();
    }
}