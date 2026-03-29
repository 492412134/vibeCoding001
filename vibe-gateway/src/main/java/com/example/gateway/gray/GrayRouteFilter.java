package com.example.gateway.gray;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * 灰度路由过滤器
 * 在 ReactiveLoadBalancerClientFilter 之前执行，直接修改 GATEWAY_REQUEST_URL_ATTR 实现灰度路由
 * 
 * 工作原理：
 * 1. 判断是否是灰度请求（白名单匹配或按权重）
 * 2. 灰度请求：直接改写 URI 为 http://v2-instance，跳过负载均衡
 * 3. 非灰度请求：将灰度标记写入 Reactor Context，由 GrayServiceInstanceListSupplier 过滤实例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrayRouteFilter implements GlobalFilter, Ordered {

    /**
     * Reactor Context 中灰度路由标记的 Key
     */
    public static final String GRAY_ROUTE_KEY = "GRAY_ROUTE";

    private final GrayRuleManager grayRuleManager;
    private final ServiceInstanceSelector serviceInstanceSelector;

    @Override
    public int getOrder() {
        // 在 ReactiveLoadBalancerClientFilter(10150) 之前执行
        // 在 RouteToRequestUrlFilter(10000) 之后执行（它设置 GATEWAY_REQUEST_URL_ATTR）
        // 在 AuthFilter(-100) 之后执行
        return 10001;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        String serviceId = route.getUri().getHost();
        if (serviceId == null) {
            return chain.filter(exchange);
        }

        // 只处理配置了灰度规则的服务
        GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceId);
        if (rule == null || !rule.isEnabled()) {
            return chain.filter(exchange);
        }

        // 获取当前请求 URI
        URI originalUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (originalUri == null) {
            return chain.filter(exchange);
        }

        // 调试：打印请求头
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        log.debug("[GrayRouteFilter] Processing service: {}, original URI: {}", serviceId, originalUri);
        log.debug("[GrayRouteFilter] X-User-Id: {}, Authorization: {}", userId, authHeader != null ? "present" : "missing");

        // 判断是否是灰度请求
        boolean isGrayRequest = grayRuleManager.shouldRouteToGray(exchange, serviceId);

        if (isGrayRequest) {
            // 选择灰度实例
            ServiceInstance instance = serviceInstanceSelector.selectInstance(serviceId, rule.getVersion());

            if (instance != null) {
                // 构建灰度 URI
                String path = originalUri.getPath();
                String query = originalUri.getQuery();
                String fullPath = path + (query != null ? "?" + query : "");

                URI grayUri = URI.create(String.format("http://%s:%d%s",
                        instance.getHost(), instance.getPort(), fullPath));

                // 关键：修改 GATEWAY_REQUEST_URL_ATTR
                // 使用 http:// 前缀，ReactiveLoadBalancerClientFilter 会自动跳过负载均衡
                exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, grayUri);

                log.info("[GrayRouteFilter] Gray route: {} -> {}:{}, version: {}",
                        serviceId, instance.getHost(), instance.getPort(), rule.getVersion());
                log.info("[GrayRouteFilter] Forwarding with headers - X-User-Id: {}, Authorization: {}",
                        userId, authHeader != null ? "present" : "missing");
            } else {
                log.warn("[GrayRouteFilter] No gray instance found for version: {}, fallback to normal", rule.getVersion());
            }
        } else {
            log.debug("[GrayRouteFilter] Normal route for service: {}", serviceId);
        }

        // 将灰度标记写入 Reactor Context
        // 非灰度请求会由 GrayServiceInstanceListSupplier 读取此标记并过滤实例
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(GRAY_ROUTE_KEY, isGrayRequest));
    }
}
