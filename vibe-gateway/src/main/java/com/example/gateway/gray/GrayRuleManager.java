package com.example.gateway.gray;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GrayRuleManager implements ApplicationListener<EnvironmentChangeEvent> {
    private final org.springframework.context.ApplicationContext applicationContext;
    private final Map<String, GrayRuleConfig.GrayRule> ruleCache = new ConcurrentHashMap<>();

    public GrayRuleManager(org.springframework.context.ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        //更新灰度发布的缓存规则；
        for (String key : event.getKeys()) {
            if (key.startsWith("gray.rules")) {
                clearCache();
                return;
            }
        }
    }

    private GrayRuleConfig getGrayRuleConfig() {
        return applicationContext.getBean(GrayRuleConfig.class);
    }

    public GrayRuleConfig.GrayRule getRule(String service) {
        GrayRuleConfig.GrayRule rule = ruleCache.get(service);
        if (rule != null) {
            return rule;
        }

        GrayRuleConfig grayRuleConfig = getGrayRuleConfig();
        if (!CollectionUtils.isEmpty(grayRuleConfig.getRules())) {
            for (GrayRuleConfig.GrayRule grayRule : grayRuleConfig.getRules()) {
                if (service.equals(grayRule.getService())) {
                    ruleCache.put(service, grayRule);
                    return grayRule;
                }
            }
        }

        return null;
    }

    public boolean shouldRouteToGray(ServerWebExchange exchange, String service) {
        GrayRuleConfig.GrayRule rule = getRule(service);
        if (rule == null || !rule.isEnabled()) {
            log.debug("[GrayRule] No rule found or rule disabled for service: {}", service);
            return false;
        }

        log.debug("[GrayRule] Found rule for service: {}, weight: {}, conditions: {}", 
                service, rule.getWeight(), 
                rule.getConditions() != null ? rule.getConditions().size() : 0);

        // 有配置条件时：白名单模式（所有条件必须同时满足）
        if (!CollectionUtils.isEmpty(rule.getConditions())) {
            boolean allMatch = true;
            for (GrayRuleConfig.GrayCondition condition : rule.getConditions()) {
                boolean match = matchCondition(exchange, condition);
                log.debug("[GrayRule] Condition type: {}, operator: {}, values: {}, match: {}",
                        condition.getType(), condition.getOperator(), condition.getValues(), match);
                if (!match) {
                    allMatch = false;
                    break;
                }
            }
            // 白名单用户100%走灰度
            if (allMatch) {
                log.debug("[GrayRule] All conditions matched, routing to gray");
                return true;
            }
        }

        // 无条件或白名单未命中时：按比例分流
        if (rule.getWeight() > 0) {
            boolean result = Math.random() * 100 < rule.getWeight();
            log.debug("[GrayRule] Weight-based routing, weight: {}, result: {}", rule.getWeight(), result);
            return result;
        }

        log.debug("[GrayRule] No weight configured, routing to normal");
        return false;
    }

    private boolean matchCondition(ServerWebExchange exchange, GrayRuleConfig.GrayCondition condition) {
        String type = condition.getType();
        String operator = condition.getOperator();
        List<String> values = condition.getValues();

        if (CollectionUtils.isEmpty(values)) {
            return false;
        }

        switch (type) {
            case "user_id":
                return matchUserId(exchange, operator, values);
            case "ip":
                return matchIp(exchange, operator, values);
            case "header":
                return matchHeader(exchange, operator, values);
            case "param":
                return matchParam(exchange, operator, values);
            default:
                return false;
        }
    }

    private boolean matchUserId(ServerWebExchange exchange, String operator, List<String> values) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null) {
            userId = exchange.getRequest().getQueryParams().getFirst("userId");
        }
        return matchValue(userId, operator, values);
    }

    private boolean matchIp(ServerWebExchange exchange, String operator, List<String> values) {
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return matchValue(ip, operator, values);
    }

    private boolean matchHeader(ServerWebExchange exchange, String operator, List<String> values) {
        if (values.size() < 2) {
            return false;
        }
        String headerName = values.get(0);
        List<String> headerValues = values.subList(1, values.size());
        String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
        return matchValue(headerValue, operator, headerValues);
    }

    private boolean matchParam(ServerWebExchange exchange, String operator, List<String> values) {
        if (values.size() < 2) {
            return false;
        }
        String paramName = values.get(0);
        List<String> paramValues = values.subList(1, values.size());
        String paramValue = exchange.getRequest().getQueryParams().getFirst(paramName);
        return matchValue(paramValue, operator, paramValues);
    }

    private boolean matchValue(String actualValue, String operator, List<String> expectedValues) {
        if (actualValue == null) {
            return false;
        }

        switch (operator) {
            case "in":
                return expectedValues.contains(actualValue);
            case "not_in":
                return !expectedValues.contains(actualValue);
            case "equals":
                return expectedValues.stream().anyMatch(actualValue::equals);
            case "not_equals":
                return expectedValues.stream().noneMatch(actualValue::equals);
            case "regex":
                return expectedValues.stream().anyMatch(pattern -> Pattern.matches(pattern, actualValue));
            default:
                return false;
        }
    }

    public void clearCache() {
        ruleCache.clear();
    }
}
