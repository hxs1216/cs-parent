package com.taoqi.tenancy.web.rest;

import com.taoqi.common.entity.DataSourceInfo;
import com.taoqi.tenancy.service.TenantDataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author tanglh
 * @Date 2018/11/28 11:54
 */
@RestController
@RequestMapping("/api/multi-tenancy")
@ConditionalOnProperty(prefix = "spring.jpa.properties.hibernate", name = {"multiTenancy", "tenant_identifier_resolver", "multi_tenant_connection_provider"})
public class TenantDataSourceResource {
    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceResource.class);

    @Autowired
    private TenantDataSourceService tenantDataSourceService;

    /**
     * 添加数据源到本地服务
     *
     * @param dataSourceInfo 数据源连接信息
     * @return
     */
    @PostMapping("/tenant/data-source")
    public ResponseEntity<Void> addDataSource(@RequestBody DataSourceInfo dataSourceInfo) {
        log.debug("REST request to addDatasource : {}", dataSourceInfo);
        tenantDataSourceService.addDataSource(dataSourceInfo);
        tenantDataSourceService.addRemoteDataSource(dataSourceInfo);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询租户数据源是否存在
     *
     * @param tenantCode 租户编码
     * @param type       数据库类型 mongo/mysql
     * @return
     */
    @GetMapping("/tenant/data-source/{tenantCode}/check/{type}")
    public ResponseEntity<Boolean> getDataSource(@PathVariable String tenantCode, @PathVariable String type) {
        log.debug("REST request to getDataSource : {},{}", tenantCode, type);
        return ResponseEntity.ok(tenantDataSourceService.checkDataSource(tenantCode, type));
    }

    /**
     * 获取所有数据源对应的租户编号 key
     *
     * @return
     */
    @GetMapping("/tenant/data-source/keys")
    public ResponseEntity<List<String>> getAllDataSourceKeys() {
        log.debug("REST request to getAllDataSource ");
        return ResponseEntity.ok(tenantDataSourceService.getAllValidDataSourceTenantCode());
    }

    /**
     * 获取所有租户数据源信息
     *
     * @return
     */
    @GetMapping("/tenant/data-source")
    public ResponseEntity<List<DataSourceInfo>> getAllDataSource() {
        log.debug("REST request to getAllDataSource ");
        return ResponseEntity.ok(tenantDataSourceService.getAllValidDataSource());
    }

}