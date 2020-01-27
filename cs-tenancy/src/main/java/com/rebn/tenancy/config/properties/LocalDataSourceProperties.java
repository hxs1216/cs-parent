package com.rebn.tenancy.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Title: 本地配置的数据源
 * Description: 本地配置的数据源
 * Create Time: 2020/1/24
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.local", ignoreInvalidFields = true, ignoreUnknownFields = true)
public class LocalDataSourceProperties {


    private Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
