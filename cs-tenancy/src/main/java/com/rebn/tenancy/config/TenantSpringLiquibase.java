package com.rebn.tenancy.config;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

/**
 * Title: 租户数据源管理
 * Description: StopWatch用于计时
 * Create Time: 2020/1/25
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
public class TenantSpringLiquibase extends SpringLiquibase {

    private final Logger logger = LoggerFactory.getLogger(TenantSpringLiquibase.class);


    @Override
    public void afterPropertiesSet() throws LiquibaseException {
        boolean canRun = null != dataSource && shouldRun;
        if (canRun) {
            logger.debug("Starting Liquibase synchronously");
            initDb();
        } else if (null == dataSource) {
            logger.warn("dataSource is empty.");
        } else {
            log.info("Liquibase is not running");
        }
    }

    private void initDb() throws LiquibaseException {

        // 开始计时
        StopWatch watch = new StopWatch();
        watch.start();
        super.afterPropertiesSet();
        watch.stop();
        logger.debug("Started Liquibase in {} ms", watch.getTotalTimeMillis());
    }
}
