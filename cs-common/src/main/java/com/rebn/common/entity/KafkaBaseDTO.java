package com.rebn.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Title: kafka基础数据传输对象
 * Description: kafka基础数据传输对象
 * Create Time: 2020/1/23
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaBaseDTO {

    /**
     * 租户号 | 业务方id
     */
    private String tenantCode;

    public String getTenantCode() {
        return tenantCode;
    }

    public KafkaBaseDTO setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"tenantCode\":\"").append(tenantCode).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
