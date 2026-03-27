package com.example.gateway.gray;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

@Slf4j
@Component
public class GrayRouteGatewayFilterFactory extends AbstractGatewayFilterFactory<GrayRouteGatewayFilterFactory.Config> {
    private final GrayRuleManager grayRuleManager;
    private final ServiceInstanceSelector serviceInstanceSelector;

    public GrayRouteGatewayFilterFactory(GrayRuleManager grayRuleManager, ServiceInstanceSelector serviceInstanceSelector) {
        super(Config.class);
        this.grayRuleManager = grayRuleManager;
        this.serviceInstanceSelector = serviceInstanceSelector;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 获取路由信息
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route == null) {
                return chain.filter(exchange);
            }

            // 获取服务ID
            String serviceId = route.getUri().getHost();
            if (serviceId == null) {
                return chain.filter(exchange);
            }

            // 检查是否需要灰度路由
            if (grayRuleManager.shouldRouteToGray(exchange, serviceId)) {
                GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceId);
                if (rule != null) {
                    // 选择灰度实例
                    var instance = serviceInstanceSelector.selectInstance(serviceId, rule.getVersion());
                    if (instance != null) {
                        // 重写请求URL
                        URI originalUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
                        if (originalUri != null) {
                            URI grayUri = URI.create("lb://" + serviceId + originalUri.getPath());
                            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, grayUri);
                            log.info("Gray routing enabled for service: {}, version: {}", serviceId, rule.getVersion());
                        }
                    }
                }
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // 可以添加配置参数
    }
}
