package com.example.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.List;

/**
 * LoadBalancer 链检查工具
 * 用于查看和验证 ServiceInstanceListSupplier 的构建链
 */
@Slf4j
@Component
public class LoadBalancerInspector {

    /**
     * 打印完整的 Supplier 链结构
     */
    public static void printSupplierChain(ServiceInstanceListSupplier supplier, String serviceId) {
        log.info("========== LoadBalancer Supplier Chain for [{}] ==========", serviceId);

        int level = 0;
        ServiceInstanceListSupplier current = supplier;

        while (current != null) {
            String indent = "  ".repeat(level);
            String className = current.getClass().getName();

            // 简化类名
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf(".") + 1);
            }

            log.info("{}{} {}", indent, level == 0 ? "▶" : "↓", className);

            // 获取功能说明
            String description = getSupplierDescription(current);
            if (description != null) {
                log.info("{}   {}", indent, description);
            }

            // 尝试获取委托的 Supplier
            ServiceInstanceListSupplier next = getDelegate(current);
            current = next;
            level++;
        }

        log.info("========== End of Chain ({} layers) ==========", level);
    }

    /**
     * 获取 Supplier 的功能描述
     */
    private static String getSupplierDescription(ServiceInstanceListSupplier supplier) {
        String className = supplier.getClass().getName();

        if (className.contains("GrayServiceInstanceListSupplier")) {
            return "【灰度过滤】根据灰度规则过滤实例列表";
        }
        if (className.contains("CachingServiceInstanceListSupplier")) {
            return "【缓存】缓存实例列表，默认TTL=35s，容量=256";
        }
        if (className.contains("DiscoveryClientServiceInstanceListSupplier")) {
            return "【服务发现】从 DiscoveryClient 获取实例列表";
        }
        if (className.contains("HealthCheckServiceInstanceListSupplier")) {
            return "【健康检查】过滤不健康的实例";
        }
        if (className.contains("ZonePreferenceServiceInstanceListSupplier")) {
            return "【区域亲和】优先选择同区域的实例";
        }

        return null;
    }

    /**
     * 尝试获取装饰器委托的 Supplier
     */
    private static ServiceInstanceListSupplier getDelegate(ServiceInstanceListSupplier supplier) {
        try {
            // 大多数装饰器都有 delegate 字段
            Field delegateField = findField(supplier.getClass(), "delegate");
            if (delegateField != null) {
                delegateField.setAccessible(true);
                Object delegate = delegateField.get(supplier);
                if (delegate instanceof ServiceInstanceListSupplier) {
                    return (ServiceInstanceListSupplier) delegate;
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return null;
    }

    /**
     * 查找字段（包括父类）
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 测试 Supplier 链是否正常工作
     */
    public static void testSupplierChain(ServiceInstanceListSupplier supplier, String serviceId) {
        log.info("========== Testing Supplier Chain for [{}] ==========", serviceId);

        try {
            Flux<List<ServiceInstance>> instancesFlux = supplier.get();
            List<ServiceInstance> instances = instancesFlux.blockFirst();

            if (instances == null || instances.isEmpty()) {
                log.warn("[{}] No instances returned!", serviceId);
            } else {
                log.info("[{}] Returned {} instances:", serviceId, instances.size());
                for (ServiceInstance instance : instances) {
                    log.info("  - {}:{} (version={})",
                            instance.getHost(),
                            instance.getPort(),
                            instance.getMetadata().get("version"));
                }
            }
        } catch (Exception e) {
            log.error("[{}] Error testing supplier chain: {}", serviceId, e.getMessage(), e);
        }

        log.info("========== End of Test ==========");
    }
}
