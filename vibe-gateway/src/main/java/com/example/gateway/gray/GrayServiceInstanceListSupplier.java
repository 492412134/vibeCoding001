package com.example.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 灰度服务实例列表供应器
 * 根据请求上下文中的灰度标记，过滤服务实例列表
 * 配合 GrayRouteFilter 使用：
 * - 灰度请求：由 GrayRouteFilter 直接路由，不经过此 Supplier
 * - 非灰度请求：经过此 Supplier，只返回 v1（非灰度）实例
 * 
 * 是不是对delegate内容进行改写后，后续的流程是否会被影响？ 
 * 答案：不会，因为 GrayServiceInstanceListSupplier 是一个装饰器，不会直接修改委托的实例列表。
 * 而是通过 Flux 的 map 操作符，在请求处理过程中动态过滤实例。
 */
@Slf4j
public class GrayServiceInstanceListSupplier implements ServiceInstanceListSupplier {
    

    private final ServiceInstanceListSupplier delegate;
    private final GrayRuleManager grayRuleManager;


    public GrayServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
                                           GrayRuleManager grayRuleManager) {
        this.delegate = delegate;
        this.grayRuleManager = grayRuleManager;
    }

    @Override
    public String getServiceId() {
        return delegate.getServiceId();
    }

    /**
     * 作用：根据灰上下文中的灰度标记，过滤服务实例列表
     */
    @Override
    public Flux<List<ServiceInstance>> get() {
        String serviceId = getServiceId();
        
        // 获取灰度规则
        GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceId);
        if (rule == null || !rule.isEnabled()) {
            // 无灰度规则，直接返回委托的实例列表
            log.debug("[GraySupplier] No gray rule for service: {}, using default supplier", serviceId);
            return delegate.get();
        }
        
        // 从 Reactor Context 获取灰度标记并过滤实例
        return Flux.deferContextual(ctx -> {
            Boolean isGrayRoute = ctx.getOrDefault(GrayRouteFilter.GRAY_ROUTE_KEY, false);
            
            return delegate.get().map(instances -> {
                if (isGrayRoute) {                    
                    //todo 后期待调整：后期空的话，根据灰度规则，返回灰度实例列表，然后走默认的负载均衡器； - 2026-03-29

                    // 灰度请求：由 GrayRouteFilter 处理，返回所有实例（实际上不会用到，因为 URI 已被改写）
                    log.debug("[GraySupplier] Gray route detected for service: {}, returning all {} instances", 
                            serviceId, instances.size());
                    return instances;
                } else {
                    // 非灰度请求：只返回非灰度版本（v1）的实例
                    List<ServiceInstance> filteredInstances = filterNonGrayInstances(instances, rule);
                    log.debug("[GraySupplier] Normal route for service: {}, filtered {} instances from {}", 
                            serviceId, filteredInstances.size(), instances.size());
                    
                    if (filteredInstances.isEmpty()) {
                        log.warn("[GraySupplier] No non-gray instances found for service: {}, fallback to all", 
                                serviceId);
                        return instances;
                    }
                    
                    return filteredInstances;
                }
            });
        });
    }

    /**
     * 过滤出非灰度版本的实例
     */
    private List<ServiceInstance> filterNonGrayInstances(List<ServiceInstance> instances,
                                                         GrayRuleConfig.GrayRule rule) {
        String grayVersion = rule.getVersion();

        return instances.stream()
                .filter(instance -> {
                    String instanceVersion = instance.getMetadata().get("version");
                    // 排除灰度版本的实例，保留 v1 或未标记版本的实例
                    return instanceVersion == null || !instanceVersion.equals(grayVersion);
                })
                .collect(Collectors.toList());
    }
}
