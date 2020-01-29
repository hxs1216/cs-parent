package com.rebn.tenancy.service;

import com.rebn.common.constants.Constants;
import com.rebn.common.entity.DataSourceInfo;
import com.rebn.common.entity.TenantDataSourceDetail;
import com.rebn.common.entity.TenantDataSourceWrapperDTO;
import com.rebn.common.errors.CommonException;
import com.rebn.common.errors.ErrorCode;
import com.rebn.common.util.IPUtil;
import com.rebn.tenancy.config.properties.LocalDataSourceProperties;
import com.rebn.tenancy.config.properties.MongoProperties;
import com.rebn.tenancy.config.properties.TenantConfigProperties;
import com.rebn.tenancy.multi.TenantDataSourceProvider;
import com.rebn.tenancy.multi.mongo.TenantMongoDatabaseProvider;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rebn.tenancy.constants.TenantRemote.UaaMethods.*;
import static org.springframework.http.HttpMethod.GET;
import static com.rebn.tenancy.config.properties.TenantConfigProperties.DEFAULT_HEADERS;
import static com.rebn.tenancy.config.properties.TenantConfigProperties.DEFAULT_REQUEST;

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

    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceService.class);

    private static final ParameterizedTypeReference<DataSourceInfo> DATA_SOURCE_INFO = new ParameterizedTypeReference<DataSourceInfo>() {
    };
    private static final ParameterizedTypeReference<List<DataSourceInfo>> LIST_DATA_SOURCE_INFO = new ParameterizedTypeReference<List<DataSourceInfo>>() {
    };

    private static final String QUERY_SCHEMA_SQL = "SELECT count(sc.SCHEMA_NAME) FROM information_schema.SCHEMATA sc where sc.SCHEMA_NAME=?";
    private static final String CREATE_DATABASE = "create database ";
    private static final String DEFAULT_CHARACTER_SET = " DEFAULT  character set ";
    private static final String MYSQL_SPEC = "`";
    private static final String USE = "use ";
    private static final String ALTER_DATABASE = "alter database ";
    private static final String CHARACTER_SET = " character set ";

    private static final String defaultCharset = " utf8mb4";

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

    /**
     * 添加数据源
     *
     * @param dataSourceInfo
     */
    public void addDataSource(DataSourceInfo dataSourceInfo) {
        log.info("addDataSource :{} ", dataSourceInfo);
        if (null == dataSourceInfo) {
            log.warn("remote datasource is empty.");
            return;
        }

        // 不覆盖、该租户已被初始化
        if (BooleanUtils.isNotTrue(dataSourceInfo.getNeedOverride()) && null != TenantDataSourceProvider.getTenantDataSource(dataSourceInfo.getTenantCode())) {
            throw new CommonException(ErrorCode.data_source_has_init);
        }
        if (Constants.PROFILE_MYSQL.equalsIgnoreCase(dataSourceInfo.getType())) {
            this.initDatabase(dataSourceInfo); //mysql
        } else if (Constants.PROFILE_MONGO.equalsIgnoreCase(dataSourceInfo.getType()) && null != mongoProperties) {
            TenantMongoDatabaseProvider.setDatabase(dataSourceInfo.getTenantCode(), dataSourceInfo.getDatabase());
        }
    }

    /**
     * 查询数据源
     *
     * @param tenantCode
     * @return
     */
    public Boolean checkDataSource(String tenantCode, String type) {
        log.info("checkDataSource :{} , {}", tenantCode, type);
        if (Constants.PROFILE_MYSQL.equalsIgnoreCase(type)) {
            return TenantDataSourceProvider.checkTenantDataSourceExist(tenantCode);
        } else if (Constants.PROFILE_MONGO.equalsIgnoreCase(type)) {
            return TenantMongoDatabaseProvider.checkDataBaseExist(tenantCode);
        }
        return Boolean.FALSE;
    }

    /**
     * 校验所有数据源是否存在
     *
     * @param tenantCode
     * @return
     */
    public Boolean checkAllDataSource(String tenantCode) {
        return TenantDataSourceProvider.checkTenantDataSourceExist(tenantCode)
                || TenantMongoDatabaseProvider.checkDataBaseExist(tenantCode);
    }

    /**
     * 查询所有数据源
     *
     * @return
     */
    public List<String> getAllValidDataSourceTenantCode() {
        log.info("getAllValidDataSourceTenantCode ");
        return new ArrayList<>(TenantDataSourceProvider.getDataSourceMap().keySet());
    }

    /**
     * 查询当前数据源列表
     *
     * @return
     */
    public List<DataSourceInfo> getAllValidDataSource() {
        log.info("getAllValidDataSource ");
        List<DataSourceInfo> result = new ArrayList<>();
        TenantDataSourceProvider.getDataSourceMap().forEach((key, value) -> {
            HikariDataSource dataSource = (HikariDataSource) value;
            DataSourceInfo dataSourceInfo = genDataSourceInfoByHikariDataSource(dataSource);
            dataSourceInfo.setTenantCode(key);
            result.add(dataSourceInfo);
        });
        return result;
    }

    /**
     * 连接基本信息
     *
     * @param dataSource
     * @return
     */
    private DataSourceInfo genDataSourceInfoByHikariDataSource(HikariDataSource dataSource) {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.url(dataSource.getJdbcUrl())
                .username(dataSource.getUsername())
                .jdbcDriver(dataSource.getDriverClassName());
        return dataSourceInfo;
    }

    /**
     * 初始化数据源
     *
     * @param dataSourceInfo
     */
    public void initDatabase(DataSourceInfo dataSourceInfo) {
        log.info("init multi database :{} ", dataSourceInfo);
        try {
            DataSource dataSource = null;
            try {
                //尝试连接租户指定数据源
                dataSource = tenantDataSourceProvider.genDataSource(dataSourceInfo);
                dataSource.getConnection();
                initDatabaseBySpringLiquibase(dataSource);
            } catch (Exception e) {
                //异常则在默认数据源同实例下创建一个数据库
                log.error("connect to tenant DB failed,{} ", e);
                final Connection defaultConnection = TenantDataSourceProvider.getTenantDataSource(TenantDataSourceProvider.DEFAULT_KEY).getConnection();
                this.useTenantDB(dataSourceInfo.getTenantCode(), dataSourceInfo.getDatabase(), defaultConnection);
                if (null == dataSource) {
                    dataSource = tenantDataSourceProvider.genDataSource(dataSourceInfo);
                }
                initDatabaseBySpringLiquibase(dataSource);
                if (null != defaultConnection) {
                    defaultConnection.close();
                }
            }
        } catch (SQLException e) {
            log.error("initDatabase failed,{}", e);
        }
    }

    /**
     * 使用租户数据库
     * 如果数据库不存在则在默认
     *
     * @param tenantIdentifier
     * @param dbName
     * @param connection
     * @return 是否需要初始化数据库
     */
    private boolean useTenantDB(String tenantIdentifier, String dbName, Connection connection) {
        if (TenantDataSourceProvider.DEFAULT_KEY.equals(tenantIdentifier)) {
            return false;
        }
        if (!StringUtils.hasText(dbName)) {
            log.error("cannot get dbName");
            return false;
        }
        boolean result = false;
        try {
            log.info("dbName:{}", dbName);
            PreparedStatement ps = connection.prepareStatement(QUERY_SCHEMA_SQL);
            ps.setString(1, dbName);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                log.info("db:{} is not exist, system auto create:{}", dbName, dbName);

                if (StringUtils.isEmpty(charset)) {
                    charset = defaultCharset;
                    log.debug("charset:{}", charset);
                }
                String createSQL = CREATE_DATABASE + MYSQL_SPEC + dbName + MYSQL_SPEC + DEFAULT_CHARACTER_SET + charset;

                log.info("createSQL:{}", createSQL);
                connection.createStatement().execute(createSQL);
                result = true;
            }
            connection.createStatement().execute(USE + MYSQL_SPEC + dbName + MYSQL_SPEC);
        } catch (Exception e) {
            log.error("connect to DB:{} failed, connection:{}, exception:{}", dbName, connection, e);
        }
        return result;
    }

    /**
     * 设置数据库编码 为 配置的
     *
     * @param dbName
     * @throws SQLException
     */
    public void setDatabaseCharsetToConfig(String dbName) {
        if (StringUtils.isEmpty(charset)) {
            charset = defaultCharset;
            log.debug("charset:{}", charset);
        }
        log.debug("setDatabaseCharsetToConfig:{}", dbName);
        try {
            final Connection defaultConnection = TenantDataSourceProvider.getTenantDataSource(TenantDataSourceProvider.DEFAULT_KEY).getConnection();
            defaultConnection.createStatement().execute(ALTER_DATABASE + MYSQL_SPEC + dbName + MYSQL_SPEC + CHARACTER_SET + charset);
            log.debug("setDatabaseCharsetToConfig success");
            defaultConnection.close();
        } catch (SQLException e) {
            log.error("setDatabaseCharsetToConfig error,", e);
        }

    }

    /**
     * 设置数据库编码为 urf8mb4
     *
     * @param dbName
     */
    public void setDatabaseCharsetToUtf8mb4(String dbName) {
        log.debug("setDatabaseCharsetToUtf8mb4:{}", dbName);
        try {
            final Connection defaultConnection = TenantDataSourceProvider.getTenantDataSource(TenantDataSourceProvider.DEFAULT_KEY).getConnection();
            defaultConnection.createStatement().execute(ALTER_DATABASE + MYSQL_SPEC + dbName + MYSQL_SPEC + CHARACTER_SET + defaultCharset);
            log.debug("setDatabaseCharsetToUtf8mb4 success");
        } catch (SQLException e) {
            log.error("setDatabaseCharsetToConfig error,", e);
        }
    }


    /**
     * 使用liquibase 初始化数据库
     *
     * @param dataSource
     */
    private void initDatabaseBySpringLiquibase(DataSource dataSource) {
        log.debug("current DataSource:{}", dataSource);
        if (null == tenantLiquibase || null == liquibaseProperties) {
            log.warn("springLiquibase is null can't init multi");
            return;
        }
        try {
            log.info("start init multi by liquibase");
            tenantLiquibase.setDataSource(dataSource);
            tenantLiquibase.afterPropertiesSet();
            log.info("success init multi by liquibase");
        } catch (Exception e) {
            log.error("springLiquibase init multi failed, {}", dataSource, e);
        }
    }

    /**
     * 同步初始化远程数据库
     */
    public void initRemoteDataSourceSync() {
        initRemoteDataSource();
    }

    /**
     * 异步初始化远程数据源
     */
    @Async
    public void initRemoteDataSourceAsync() {
        initRemoteDataSource();
    }

    /**
     * 初始化远程数据源
     */
    private void initRemoteDataSource() {
        if (initLocalTenantDataSource()) {
            log.info("has init local success.");
            return;
        }

        if (null == REST_TEMPLATE) {
            log.warn("tenantConfigCenterRemoteProxy is null can't get dataSource from remote.");
            return;
        }
        if (StringUtils.isEmpty(APP_NAME)) {
            log.warn("server Name is null can't init remote dataSource");
            return;
        }
        try {
            List<DataSourceInfo> dataSourceInfoList = this.getRemoteDataSourceFromUaa();
            if (CollectionUtils.isEmpty(dataSourceInfoList)) {
                log.warn("remote dataSource is empty");
                return;
            }
            dataSourceInfoList.forEach(dataSourceInfo -> {
                dataSourceInfo.setNeedOverride(true);
                addRemoteDataSource(dataSourceInfo);
                if (Constants.PROFILE_MYSQL.equalsIgnoreCase(dataSourceInfo.getType())) {
                    initDatabaseBySpringLiquibase(TenantDataSourceProvider.getTenantDataSource(dataSourceInfo.getTenantCode()));
                }
            });
        } catch (Exception e) {
            log.error("init remote dataSource error. {}", e);
        }
    }

    /**
     * 初始化本地配置的数据库
     *
     * @return true 已初始化本地数据源  则不会初始化远程数据源
     */
    private boolean initLocalTenantDataSource() {
        if (null != localDataSourceProperties.getProperties() && localDataSourceProperties.getProperties().size() > 0) {
            log.info("init local tenant dataSource");
            HikariDataSource defaultDataSource = (HikariDataSource) TenantDataSourceProvider.getTenantDataSource(TenantDataSourceProvider.DEFAULT_KEY);
            if (null == defaultDataSource) {
                log.info("default dataSource is empty");
                return false;
            }
            localDataSourceProperties.getProperties().forEach((key, value) -> {
                DataSourceInfo localDataSourceInfo = new DataSourceInfo();
                localDataSourceInfo.needOverride(Boolean.FALSE)
                        .tenantCode(key)
                        .url(value)
                        .database(TenantDataSourceProvider.getDatabaseByUrl(value))
                        .username(defaultDataSource.getUsername())
                        .password(defaultDataSource.getPassword())
                        .setServerName(APP_NAME);
                localDataSourceInfo.setType(Constants.PROFILE_MYSQL);
                addRemoteDataSource(localDataSourceInfo);
                initDatabaseBySpringLiquibase(TenantDataSourceProvider.getTenantDataSource(localDataSourceInfo.getTenantCode()));
            });
            return true;
        }
        return false;
    }

    /**
     * 从远程获取数据源
     *
     * @return
     */
    public List<DataSourceInfo> getRemoteDataSourceFromUaa() {
        return REST_TEMPLATE.exchange(getRequestUri(QUERY_ALL_DATA_SOURCE, APP_NAME), GET, DEFAULT_REQUEST, LIST_DATA_SOURCE_INFO).getBody();
    }

    /**
     * 根据租户编码初始化远程配置
     *
     * @param tenantCode
     */
    public void initDataSourceInfoByTenantCode(String tenantCode, String type) {
        if (null == REST_TEMPLATE) {
            log.warn("tenantConfigCenterRemoteProxy is null can't get dataSource from remote.");
            return;
        }
        if (StringUtils.isEmpty(APP_NAME) || StringUtils.isEmpty(tenantCode)) {
            log.warn("server Name is null can't init remote dataSource");
            return;
        }
        DataSourceInfo result = REST_TEMPLATE.exchange(getRequestUri(QUERY_TENANT_DATA_SOURCE, tenantCode, APP_NAME, type), GET, DEFAULT_REQUEST, DATA_SOURCE_INFO).getBody();
        addRemoteDataSource(result);
    }

    /**
     * 定时任务每分钟执行上报数据源状态
     */
    @Scheduled(initialDelay = 1000 * 60, fixedRate = 1000 * 60)
    public void tenantDataSourceStatusTask() {
        log.debug("tenantDataSourceStatusTask");
        if (null != localDataSourceProperties.getProperties() && localDataSourceProperties.getProperties().size() > 0) {
            log.info("has init local success. no need sync remote config");
            return;
        }
        if (null == REST_TEMPLATE) {
            log.warn("tenantConfigCenterRemoteProxy is null can't send dataSource to remote.");
            return;
        }
        if (StringUtils.isEmpty(APP_NAME)) {
            log.warn("server Name is null can't send dataSource to remote.");
            return;
        }
        HttpEntity<TenantDataSourceWrapperDTO> formEntity = new HttpEntity<>(genAllValidTenantDataSourceWrapper(), DEFAULT_HEADERS);
        ResponseEntity<List<DataSourceInfo>> response = REST_TEMPLATE.exchange(getRequestUri(TENANT_DATA_SOURCE_STATUS, APP_NAME), HttpMethod.POST, formEntity, LIST_DATA_SOURCE_INFO);
        log.debug("response:{}", response);
        if (response.getStatusCode().value() < HttpStatus.OK.value() || response.getStatusCode().value() > HttpStatus.MULTIPLE_CHOICES.value()) {
            log.warn("send dataSource to remote error.");
            //FIXME 异常告警通知
        } else if (!CollectionUtils.isEmpty(response.getBody())) {
            log.debug("send dataSource to remote success.");
            List<DataSourceInfo> dataSourceInfoList = response.getBody();
            dataSourceInfoList.forEach(this::addRemoteDataSource);
            Map<String, DataSourceInfo> dataSourceInfoMap = dataSourceInfoList.stream().collect(Collectors.toMap(DataSourceInfo::getTenantCode, Function.identity(), (key1, key2) -> key2));
            TenantDataSourceProvider.getDataSourceMap().keySet().forEach(key -> {
                if (null == dataSourceInfoMap.get(key) && !TenantDataSourceProvider.getInitList().containsKey(key)) {
                    TenantDataSourceProvider.removeDataSource(key);
                }
            });
        } else {
            log.warn("remote response is empty.");
        }
    }

    /**
     * 远程数据库添加到本地
     *
     * @param dataSourceInfo
     */
    public void addRemoteDataSource(DataSourceInfo dataSourceInfo) {
        if (null == dataSourceInfo) {
            return;
        }
        if (Constants.PROFILE_MYSQL.equalsIgnoreCase(dataSourceInfo.getType())) {
            if (!checkDataSource(dataSourceInfo.getTenantCode(), dataSourceInfo.getType())) {
                tenantDataSourceProvider.addDataSource(dataSourceInfo);
            }
        } else if (Constants.PROFILE_MONGO.equalsIgnoreCase(dataSourceInfo.getType()) && null != mongoProperties) {
            TenantMongoDatabaseProvider.setDatabase(dataSourceInfo.getTenantCode(), dataSourceInfo.getDatabase());
        }
    }


    /**
     * 获取请求url
     *
     * @param method
     * @param replace
     * @return
     */
    private String getRequestUri(String method, Object... replace) {
        if (null == replace) {
            return BASE_URL + method;
        }
        return BASE_URL + String.format(method, replace);
    }

    /**
     * 获取有效配置
     *
     * @return
     */
    private TenantDataSourceWrapperDTO genAllValidTenantDataSourceWrapper() {
        log.info("genAllValidTenantDataSourceDetail ");
        List<TenantDataSourceDetail> resultList = new ArrayList<>();
        resultList.add(genAllValidTenantDataSourceDetail4MySql());
        if (null != mongoProperties) {
            resultList.add(genAllValidTenantDataSourceDetail4Mongo());
        }
        TenantDataSourceWrapperDTO result = new TenantDataSourceWrapperDTO();
        result.setTenantDataSourceDetailList(resultList);
        result.setIp(IPUtil.getServerIp());
        result.setInstanceId(instanceId);
        return result;
    }

    /**
     * 获取MYSQL数据源明细
     *
     * @return
     */
    private TenantDataSourceDetail genAllValidTenantDataSourceDetail4MySql() {
        TenantDataSourceDetail result = new TenantDataSourceDetail();
        Map<String, Object> dataSourceMap = new HashMap<>(4);

        TenantDataSourceProvider.getDataSourceMap().forEach((key, value) -> {
            HikariDataSource dataSource = (HikariDataSource) value;
            DataSourceInfo dataSourceInfo = genDataSourceInfoByHikariDataSource(dataSource);
            dataSourceInfo.tenantCode(key)
                    .password(dataSource.getPassword())
                    .serverName(APP_NAME)
                    .database(TenantDataSourceProvider.getDatabase(key));
            dataSourceInfo.setType(Constants.PROFILE_MYSQL);
            dataSourceMap.put(key, dataSourceInfo);
            Map<String, Object> objectMap = new HashMap<>(4);
            objectMap.put("poolName", dataSource.getPoolName());
            objectMap.put("dataSourceJNDI", dataSource.getDataSourceJNDI());
            objectMap.put("healthCheckRegistry", dataSource.getHealthCheckRegistry());
            objectMap.put("connectionInitSql", dataSource.getConnectionInitSql());
            objectMap.put("dataSourceClassName", dataSource.getDataSourceClassName());
            objectMap.put("connectionTimeout", dataSource.getConnectionTimeout());
            objectMap.put("connectionTestQuery", dataSource.getConnectionTestQuery());
            try {
                objectMap.put("loginTimeout", dataSource.getLoginTimeout());
            } catch (SQLException e) {
                log.warn("get loginTimeout failed");
            }
            if (null != dataSource.getDataSourceProperties()) {
                dataSource.getDataSourceProperties().forEach((k, v) -> objectMap.put(String.valueOf(k), v));
            }
            if (null != dataSource.getHealthCheckProperties()) {
                dataSource.getHealthCheckProperties().forEach((k, v) -> objectMap.put(String.valueOf(k), v));
            }
            Properties properties = new Properties();
            objectMap.forEach((k, v) -> {
                if (null != v) {
                    properties.put(k, String.valueOf(v));
                }
            });
            dataSourceInfo.setProperties(properties);
        });
        result.setType(Constants.PROFILE_MYSQL);
        result.setDataSourceMap(dataSourceMap);
        result.setProperties(new Properties());
        return result;
    }

    /**
     * 获取MONGO数据源明细
     *
     * @return
     */
    private TenantDataSourceDetail genAllValidTenantDataSourceDetail4Mongo() {
        if (null == mongoProperties) {
            return null;
        }
        TenantDataSourceDetail result = new TenantDataSourceDetail();
        Properties properties = new Properties();

        Map<String, Object> objectMap = new HashMap<>(4);
        objectMap.put("defaultDbName", mongoProperties.getDbName());
        objectMap.put("host", mongoProperties.getHost());
        objectMap.put("name", mongoProperties.getName());
        objectMap.put("password", mongoProperties.getPassword());
        objectMap.put("replicaSet", mongoProperties.getReplicaSet());
        objectMap.put("uri", mongoProperties.getUri());
        objectMap.forEach((k, v) -> {
            if (null != v) {
                properties.put(k, String.valueOf(v));
            }
        });
        Map<String, Object> dataSourceMap = new HashMap<>(4);

        TenantMongoDatabaseProvider.getDatabaseIndexMap().forEach((key, value) -> {
            Properties base = new Properties();
            base.putAll(properties);
            base.put(key, value);
            dataSourceMap.put(key, base);
        });
        result.setDataSourceMap(dataSourceMap);
        result.setType(Constants.PROFILE_MONGO);
        result.setProperties(properties);
        return result;
    }
}