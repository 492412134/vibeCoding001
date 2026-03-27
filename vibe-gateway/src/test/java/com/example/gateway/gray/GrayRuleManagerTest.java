package com.example.gateway.gray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GrayRuleManagerTest {
    private GrayRuleManager grayRuleManager;
    private GrayRuleConfig grayRuleConfig;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;

    @BeforeEach
    void setUp() {
        // 初始化配置
        grayRuleConfig = new GrayRuleConfig();
        List<GrayRuleConfig.GrayRule> rules = new ArrayList<>();
        
        // 创建测试规则
        GrayRuleConfig.GrayRule rule = new GrayRuleConfig.GrayRule();
        rule.setService("vibe-payment-service");
        rule.setEnabled(true);
        rule.setWeight(100);
        
        // 添加条件
        List<GrayRuleConfig.GrayCondition> conditions = new ArrayList<>();
        GrayRuleConfig.GrayCondition condition = new GrayRuleConfig.GrayCondition();
        condition.setType("user_id");
        condition.setOperator("in");
        condition.setValues(List.of("1001", "1002"));
        conditions.add(condition);
        
        rule.setConditions(conditions);
        rule.setVersion("v2");
        rules.add(rule);
        
        grayRuleConfig.setRules(rules);
        
        // 初始化管理器
        grayRuleManager = new GrayRuleManager(grayRuleConfig);
        
        // 模拟请求
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        
        Mockito.when(exchange.getRequest()).thenReturn(request);
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
    }

    @Test
    void testGetRule() {
        // 测试获取规则
        GrayRuleConfig.GrayRule rule = grayRuleManager.getRule("vibe-payment-service");
        assertNotNull(rule);
        assertEquals("vibe-payment-service", rule.getService());
        assertEquals("v2", rule.getVersion());
    }

    @Test
    void testShouldRouteToGray_WithMatchingCondition() {
        // 设置匹配的用户ID
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "1001");
        when(request.getHeaders()).thenReturn(headers);
        
        // 测试应该路由到灰度
        boolean result = grayRuleManager.shouldRouteToGray(exchange, "vibe-payment-service");
        assertTrue(result);
    }

    @Test
    void testShouldRouteToGray_WithNonMatchingCondition() {
        // 设置不匹配的用户ID
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "9999");
        when(request.getHeaders()).thenReturn(headers);
        
        // 测试不应该路由到灰度
        boolean result = grayRuleManager.shouldRouteToGray(exchange, "vibe-payment-service");
        assertFalse(result);
    }

    @Test
    void testShouldRouteToGray_DisabledRule() {
        // 修改规则为禁用
        grayRuleConfig.getRules().get(0).setEnabled(false);
        grayRuleManager.clearCache();
        
        // 测试不应该路由到灰度
        boolean result = grayRuleManager.shouldRouteToGray(exchange, "vibe-payment-service");
        assertFalse(result);
    }

    @Test
    void testShouldRouteToGray_ZeroWeight() {
        // 修改权重为0
        grayRuleConfig.getRules().get(0).setWeight(0);
        grayRuleManager.clearCache();
        
        // 测试不应该路由到灰度
        boolean result = grayRuleManager.shouldRouteToGray(exchange, "vibe-payment-service");
        assertFalse(result);
    }

    @Test
    void testShouldRouteToGray_NoRule() {
        // 测试不存在的服务
        boolean result = grayRuleManager.shouldRouteToGray(exchange, "non-existent-service");
        assertFalse(result);
    }

    @Test
    void testClearCache() {
        // 先获取规则，缓存会被填充
        grayRuleManager.getRule("vibe-payment-service");
        
        // 清除缓存
        grayRuleManager.clearCache();
        
        // 再次获取规则，应该重新从配置中加载
        GrayRuleConfig.GrayRule rule = grayRuleManager.getRule("vibe-payment-service");
        assertNotNull(rule);
    }
}
