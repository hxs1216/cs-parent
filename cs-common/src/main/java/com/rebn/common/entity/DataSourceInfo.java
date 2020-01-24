package com.rebn.common.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Properties;

/**
 * Title: 数据库链接信息
 * Description: 数据库链接信息
 * Create Time: 2020/1/23
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSourceInfo extends KafkaBaseDTO {


    /**
     * 数据库链接 url
     */
    private String url;

    /**
     * 数据库
     */
    private String database;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 连接驱动
     */
    private String jdbcDriver;

    /**
     * 是否覆盖
     */
    private Boolean needOverride;

    /**
     * 服务名
     */
    private String serverName;

    /**
     * 数据库类型 mongo/mysql
     */
    private String type;

    /**
     * 连接配置信息
     */
    private Properties properties;


    public String getDatabase() {
        return database;
    }

    public DataSourceInfo database(String database) {
        this.database = database;
        return this;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUrl() {
        return url;
    }

    public DataSourceInfo url(String url) {
        this.url = url;
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public DataSourceInfo username(String username) {
        this.username = username;
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public DataSourceInfo password(String password) {
        this.password = password;
        return this;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public DataSourceInfo jdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public Boolean getNeedOverride() {
        return needOverride;
    }

    public DataSourceInfo needOverride(Boolean needOverride) {
        this.needOverride = needOverride;
        return this;
    }

    public void setNeedOverride(Boolean needOverride) {
        this.needOverride = needOverride;
    }

    public String getServerName() {
        return serverName;
    }

    public DataSourceInfo serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DataSourceInfo tenantCode(String tenantCode) {
        super.setTenantCode(tenantCode);
        return this;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"url\":\"").append(url).append('\"');
        sb.append(",\"database\":\"").append(database).append('\"');
        sb.append(",\"username\":\"").append(username).append('\"');
        sb.append(",\"password\":\"").append(password).append('\"');
        sb.append(",\"jdbcDriver\":\"").append(jdbcDriver).append('\"');
        sb.append(",\"needOverride\":").append(needOverride);
        sb.append(",\"serverName\":\"").append(serverName).append('\"');
        sb.append(",\"type\":\"").append(type).append('\"');
        sb.append('}');
        sb.append(super.toString());
        return sb.toString();
    }
}
