package com.example.payment.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 负载均衡演示控制器
 * 
 * 用于演示Gateway的负载均衡效果
 * 当启动多个支付服务实例时，每次请求会由Gateway轮询分发到不同实例
 * 通过返回的端口号可以验证负载均衡是否生效
 */
@RestController
@RequestMapping("/api/payment")
public class LoadBalanceDemoController {

    @Autowired
    private Environment environment;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 获取当前服务实例信息
     * 用于验证Gateway负载均衡是否生效
     */
    @GetMapping("/instance")
    public Map<String, Object> getInstanceInfo() {
        Map<String, Object> info = new HashMap<>();
        
        String port = environment.getProperty("local.server.port", "unknown");
        String instanceId = applicationName + ":" + port;
        
        info.put("applicationName", applicationName);
        info.put("port", port);
        info.put("instanceId", instanceId);
        info.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        info.put("message", "当前请求由服务实例处理: " + instanceId);
        
        return info;
    }

    /**
     * 负载均衡测试接口
     * 返回当前处理请求的服务实例信息
     */
    @GetMapping("/lb-test")
    public Map<String, Object> loadBalanceTest() {
        Map<String, Object> result = new HashMap<>();
        
        String port = environment.getProperty("local.server.port", "unknown");
        
        result.put("service", applicationName);
        result.put("port", port);
        result.put("status", "UP");
        result.put("time", System.currentTimeMillis());
        
        return result;
    }
}
