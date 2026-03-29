package com.example.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 灰度负载均衡配置
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class GrayLoadBalancerConfiguration {

    @Bean
    public ServiceInstanceListSupplier serviceInstanceListSupplier(
            ConfigurableApplicationContext context,
            GrayRuleManager grayRuleManager,
            Environment environment) {

        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        if (serviceId == null) {
            serviceId = environment.getProperty("spring.application.name");
        }

        log.info("[GrayLB] Building ServiceInstanceListSupplier chain for: {}", serviceId);

        ServiceInstanceListSupplier delegate = ServiceInstanceListSupplier.builder()
                .withDiscoveryClient()
                .withCaching()
                .build(context);

        return new GrayServiceInstanceListSupplier(delegate, grayRuleManager);
    }
}
