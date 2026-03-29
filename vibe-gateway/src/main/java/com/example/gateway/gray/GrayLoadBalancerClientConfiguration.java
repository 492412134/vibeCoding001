package com.example.gateway.gray;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * 灰度负载均衡客户端配置
 *
 * 为所有服务启用灰度负载均衡配置
 * 灰度功能通过 GrayFilter 在路由级别控制
 *
 * 使用方式（在 application.yml 中配置路由）：
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: vibe-payment-service
 *           uri: lb://vibe-payment-service
 *           predicates:
 *             - Path=/api/payment/**
 *           filters:
 *             - Gray=true  # 启用灰度路由
 *
 * 灰度规则通过 Nacos 配置：
 * gray:
 *   rules:
 *     - service: vibe-payment-service
 *       enabled: true
 *       version: v2
 *       weight: 30
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients(defaultConfiguration = GrayLoadBalancerConfiguration.class)
public class GrayLoadBalancerClientConfiguration {
    // 使用 @LoadBalancerClients 注解为所有服务启用灰度配置
    // 实际灰度过滤由 GrayServiceInstanceListSupplier 处理
    // 但只在请求经过 GrayFilter 时才应用灰度逻辑
}
