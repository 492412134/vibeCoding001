package com.example.gateway.gray;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/api/gray")
@RequiredArgsConstructor
public class GrayConfigController {
    //这里不应该依赖GrayRuleConfig组件，无法触发nacos自动刷新
    private final GrayRuleConfig grayRuleConfig;

    private final GrayRuleManager grayRuleManager;
    private final DiscoveryClient discoveryClient;
    private final ServiceInstanceSelector serviceInstanceSelector;

     private final ApplicationContext applicationContext;  // 注入容器


    @GetMapping("/rules2")
   public Map<String, Object> getGrayRules2() {
        // 每次都从容器中获取最新的 GrayRuleConfig
        GrayRuleConfig config = applicationContext.getBean(GrayRuleConfig.class);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rules", config.getRules());
        return result;
    }

    @GetMapping("/rules")
    public Map<String, Object> getGrayRules() {
        GrayRuleConfig config = applicationContext.getBean(GrayRuleConfig.class);
        Map<String, Object> result = new HashMap<>();
        result.put("rules", config.getRules());
        return result;
    }

    @GetMapping("/services")
    public Map<String, Object> getServices() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();
        
        List<String> serviceNames = discoveryClient.getServices();
        for (String serviceName : serviceNames) {
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("serviceName", serviceName);
            
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            List<Map<String, Object>> instanceList = new ArrayList<>();
            
            Map<String, Integer> versionCounts = new HashMap<>();
            
            for (ServiceInstance instance : instances) {
                Map<String, Object> instanceInfo = new HashMap<>();
                instanceInfo.put("host", instance.getHost());
                instanceInfo.put("port", instance.getPort());
                instanceInfo.put("uri", instance.getUri().toString());
                
                String version = instance.getMetadata().get("version");
                if (version == null) {
                    version = "v1";
                }
                instanceInfo.put("version", version);
                instanceInfo.put("metadata", instance.getMetadata());
                
                instanceList.add(instanceInfo);
                versionCounts.merge(version, 1, Integer::sum);
            }
            
            serviceInfo.put("instances", instanceList);
            serviceInfo.put("instanceCount", instances.size());
            serviceInfo.put("versionDistribution", versionCounts);
            
            GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceName);
            if (rule != null) {
                Map<String, Object> ruleInfo = new HashMap<>();
                ruleInfo.put("enabled", rule.isEnabled());
                ruleInfo.put("weight", rule.getWeight());
                ruleInfo.put("version", rule.getVersion());
                ruleInfo.put("conditions", rule.getConditions());
                serviceInfo.put("grayRule", ruleInfo);
            } else {
                serviceInfo.put("grayRule", null);
            }
            
            services.add(serviceInfo);
        }
        
        result.put("services", services);
        result.put("totalServices", serviceNames.size());
        return result;
    }

    // @GetMapping("/logs")
    // public Map<String, String> getRouteLogs() {
    //     return GrayRouteGlobalFilter.getRouteLogs();
    // }

    // @GetMapping("/stats")
    // public Map<String, Object> getRouteStats() {
    //     Map<String, Object> result = new HashMap<>();
    //     RouteStats stats = GrayRouteGlobalFilter.getRouteStats();
        
    //     result.put("totalRequests", stats.getTotalRequests());
    //     result.put("grayRequests", stats.getGrayRequests());
    //     result.put("normalRequests", stats.getNormalRequests());
    //     result.put("versionCounts", stats.getAllVersionCounts());
        
    //     double grayPercentage = stats.getTotalRequests() > 0 
    //         ? (double) stats.getGrayRequests() / stats.getTotalRequests() * 100 
    //         : 0;
    //     result.put("grayPercentage", String.format("%.2f%%", grayPercentage));
        
    //     return result;
    // }

    // @DeleteMapping("/logs")
    // public Map<String, String> clearRouteLogs() {
    //     GrayRouteGlobalFilter.clearRouteLogs();
    //     Map<String, String> result = new HashMap<>();
    //     result.put("message", "Route logs cleared successfully");
    //     return result;
    // }

    // @PostMapping("/stats/reset")
    // public Map<String, String> resetRouteStats() {
    //     GrayRouteGlobalFilter.getRouteStats().reset();
    //     Map<String, String> result = new HashMap<>();
    //     result.put("message", "Route stats reset successfully");
    //     return result;
    // }

    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 测试灰度路由判断
     */
    @GetMapping("/test")
    public Map<String, Object> testGrayRoute(
            @RequestParam String serviceId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String ip) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取规则
        GrayRuleConfig.GrayRule rule = grayRuleManager.getRule(serviceId);
        if (rule == null) {
            result.put("error", "No rule found for service: " + serviceId);
            return result;
        }
        
        result.put("serviceId", serviceId);
        result.put("rule", rule);
        result.put("testUserId", userId);
        result.put("testIp", ip);
        
        // 检查条件匹配
        if (rule.getConditions() != null) {
            List<Map<String, Object>> conditionResults = new ArrayList<>();
            for (GrayRuleConfig.GrayCondition condition : rule.getConditions()) {
                Map<String, Object> cr = new HashMap<>();
                cr.put("type", condition.getType());
                cr.put("operator", condition.getOperator());
                cr.put("values", condition.getValues());
                
                // 手动检查匹配
                boolean match = checkCondition(condition, userId, ip);
                cr.put("match", match);
                conditionResults.add(cr);
            }
            result.put("conditionChecks", conditionResults);
        }
        
        return result;
    }
    
    private boolean checkCondition(GrayRuleConfig.GrayCondition condition, String userId, String ip) {
        if (condition.getValues() == null || condition.getValues().isEmpty()) {
            return false;
        }
        
        switch (condition.getType()) {
            case "user_id":
                if (userId == null) return false;
                return condition.getValues().contains(userId);
            case "ip":
                if (ip == null) return false;
                // 简化处理，实际应该用正则
                return condition.getValues().stream().anyMatch(pattern -> 
                    ip.matches(pattern.replace("\\", "\\").replace(".*", ".*")));
            default:
                return false;
        }
    }
}
