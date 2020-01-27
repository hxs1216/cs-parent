package com.rebn.tenancy.config.properties;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


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
@Component
public class TenantConfigProperties {
    public static final HttpHeaders DEFAULT_HEADERS = new HttpHeaders();
    public static final HttpEntity<Void> DEFAULT_REQUEST = new HttpEntity<>(DEFAULT_HEADERS);

    @Value("${eureka.instance.appname}")
    private String appName;

    /**
     * UAA
     */
    @Value("${uaa.security.client-authorization.uaa-uri:http://uaaaccount}")
    private String uaaBaseUrl;

    @Value("${uaa.security.client-authorization.allowed-ribbon:true}")
    private boolean uaaAllowedRibbon;

    /**
     * 多租户映射服务
     */
    @Value("${tenancy.client.uri:http://tenancy}")
    private String tenancyBaseUrl;

    @Value("${tenancy.client.allowed-ribbon:true}")
    private boolean tenancyAllowedRibbon;

    @Autowired
    @Qualifier("tenantLoadBalancedRestTemplate")
    private RestTemplate tenantLoadBalancedRestTemplate;

    @Autowired
    @Qualifier("tenantVanillaRestTemplate")
    private RestTemplate tenantVanillaRestTemplate;

    public String getAppName() {
        return appName;
    }

    public String getUaaBaseUrl() {
        return uaaBaseUrl;
    }

    public boolean isUaaAllowedRibbon() {
        return uaaAllowedRibbon;
    }


    public String getTenancyBaseUrl() {
        return tenancyBaseUrl;
    }

    public boolean isTenancyAllowedRibbon() {
        return tenancyAllowedRibbon;
    }

    public RestTemplate getUaaRestTemplate() {
        return genRestTemplate(uaaBaseUrl, uaaAllowedRibbon);
    }

    public RestTemplate getTenancyRestTemplate() {
        return genRestTemplate(tenancyBaseUrl, tenancyAllowedRibbon);
    }

    private RestTemplate genRestTemplate(String baseUrl, Boolean allowedRibbon) {
        if (BooleanUtils.isTrue(allowedRibbon)) {
            return tenantLoadBalancedRestTemplate;
        } else {
            return tenantVanillaRestTemplate;
        }
    }
}
