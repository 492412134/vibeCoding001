package com.example.gateway.gray;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class GrayRuleManager {
    private final GrayRuleConfig grayRuleConfig;
    private final Map<String, GrayRuleConfig.GrayRule> ruleCache = new ConcurrentHashMap<>();

    public GrayRuleManager(GrayRuleConfig grayRuleConfig) {
        this.grayRuleConfig = grayRuleConfig;
    }

    public GrayRuleConfig.GrayRule getRule(String service) {
        // 先从缓存获取
        GrayRuleConfig.GrayRule rule = ruleCache.get(service);
        if (rule != null) {
            return rule;
        }

        // 从配置中查找
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
        if (rule == null || !rule.isEnabled() || rule.getWeight() <= 0) {
            return false;
        }

        // 检查条件匹配
        if (!CollectionUtils.isEmpty(rule.getConditions())) {
            for (GrayRuleConfig.GrayCondition condition : rule.getConditions()) {
                if (!matchCondition(exchange, condition)) {
                    return false;
                }
            }
        }

        // 检查权重
        if (rule.getWeight() >= 100) {
            return true;
        }

        return Math.random() * 100 < rule.getWeight();
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

    // 清除缓存，用于配置更新时
    public void clearCache() {
        ruleCache.clear();
    }
}
