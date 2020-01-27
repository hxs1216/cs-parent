package com.rebn.tenancy.annotation.aop;

import com.rebn.common.entity.KafkaBaseDTO;
import com.rebn.common.util.JsonUtil;
import com.rebn.common.util.ThreadTenantUtil;
import com.rebn.tenancy.multi.TenantDataSourceProvider;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Title: 商户kafka监听切面处理
 * Description: 商户kafka监听切面处理
 * Create Time: 2020/1/25
 *
 * @author hxs
 * Update Time:
 * Updater:
 * Update Comments:
 */
@Component
@Aspect
public class TenantKafkaListennerAspect {

    private final Logger log = LoggerFactory.getLogger(TenantKafkaListennerAspect.class);

    @Pointcut("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public void tenantKafkaListenerAspect() {
    }

    @Before("tenantKafkaListenerAspect()")
    public void before(JoinPoint joinPoint) {
        log.debug(" invoking method {} before. ",
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
        Object[] args = joinPoint.getArgs();
        if (null == args) {
            return;
        }
        for (Object params : args) {
            setTenantCodeByHeader(params);
        }
    }

    /**
     * 设置租户信息
     */
    private void setTenantCodeByHeader(Object params) {
        if (params instanceof ConsumerRecord) {
            ConsumerRecord<?, Object> record = (ConsumerRecord<?, Object>) params;
            String tenantCode = null;

            // 尝试从value 获取租户号
            if (record.value() instanceof String) {
                Optional<String> kafkaMessage = Optional.ofNullable((String) record.value());
                if (kafkaMessage.isPresent()) {
                    KafkaBaseDTO recordValue = JsonUtil.stringToObject(kafkaMessage.get(), KafkaBaseDTO.class);
                    if (null != recordValue) {
                        tenantCode = recordValue.getTenantCode();
                    }
                }
            } else if (record.value() instanceof KafkaBaseDTO) {
                Optional<KafkaBaseDTO> kafkaMessage = Optional.ofNullable((KafkaBaseDTO) record.value());
                if (kafkaMessage.isPresent()) {
                    tenantCode = kafkaMessage.get().getTenantCode();
                }
            }
            ThreadTenantUtil.setTenant(tenantCode);
        }
    }

    /**
     * 切面执行后
     */
    @After("tenantKafkaListenerAspect()")
    public void after(JoinPoint joinPoint) {
        log.debug("invoking method {} after.",
                ((MethodSignature) joinPoint.getSignature()).getMethod().getName());
        if (null != ThreadTenantUtil.getTenant()) {
            TenantDataSourceProvider.removeInitList(ThreadTenantUtil.getTenant());
            ThreadTenantUtil.remove();
        }
    }

}