package com.example.gateway.gray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceInstanceSelectorTest {
    private ServiceInstanceSelector selector;
    private DiscoveryClient discoveryClient;

    @BeforeEach
    void setUp() {
        discoveryClient = mock(DiscoveryClient.class);
        selector = new ServiceInstanceSelector(discoveryClient);
    }

    @Test
    void testSelectInstance_WithMatchingVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        
        // v1版本实例
        ServiceInstance instance1 = createInstance("v1");
        instances.add(instance1);
        
        // v2版本实例
        ServiceInstance instance2 = createInstance("v2");
        instances.add(instance2);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试选择v2版本实例
        ServiceInstance selected = selector.selectInstance("test-service", "v2");
        assertNotNull(selected);
        assertEquals("v2", selected.getMetadata().get("version"));
    }

    @Test
    void testSelectInstance_WithNoMatchingVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        ServiceInstance instance1 = createInstance("v1");
        instances.add(instance1);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试选择不存在的版本
        ServiceInstance selected = selector.selectInstance("test-service", "v3");
        assertNull(selected);
    }

    @Test
    void testSelectInstance_WithNoInstances() {
        when(discoveryClient.getInstances("test-service")).thenReturn(new ArrayList<>());
        
        // 测试无实例情况
        ServiceInstance selected = selector.selectInstance("test-service", "v1");
        assertNull(selected);
    }

    @Test
    void testSelectDefaultInstance_WithDefaultVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        
        // v1版本实例（默认版本）
        ServiceInstance instance1 = createInstance("v1");
        instances.add(instance1);
        
        // v2版本实例
        ServiceInstance instance2 = createInstance("v2");
        instances.add(instance2);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试选择默认实例
        ServiceInstance selected = selector.selectDefaultInstance("test-service");
        assertNotNull(selected);
        assertEquals("v1", selected.getMetadata().get("version"));
    }

    @Test
    void testSelectDefaultInstance_WithNoDefaultVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        
        // 只有v2版本实例
        ServiceInstance instance1 = createInstance("v2");
        instances.add(instance1);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试无默认版本情况
        ServiceInstance selected = selector.selectDefaultInstance("test-service");
        assertNotNull(selected);
    }

    @Test
    void testSelectDefaultInstance_WithNoInstances() {
        when(discoveryClient.getInstances("test-service")).thenReturn(new ArrayList<>());
        
        // 测试无实例情况
        ServiceInstance selected = selector.selectDefaultInstance("test-service");
        assertNull(selected);
    }

    @Test
    void testHasVersionInstances_WithMatchingVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        ServiceInstance instance1 = createInstance("v1");
        instances.add(instance1);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试存在指定版本实例
        boolean result = selector.hasVersionInstances("test-service", "v1");
        assertTrue(result);
    }

    @Test
    void testHasVersionInstances_WithNoMatchingVersion() {
        // 创建测试实例
        List<ServiceInstance> instances = new ArrayList<>();
        ServiceInstance instance1 = createInstance("v1");
        instances.add(instance1);
        
        when(discoveryClient.getInstances("test-service")).thenReturn(instances);
        
        // 测试不存在指定版本实例
        boolean result = selector.hasVersionInstances("test-service", "v2");
        assertFalse(result);
    }

    @Test
    void testHasVersionInstances_WithNoInstances() {
        when(discoveryClient.getInstances("test-service")).thenReturn(new ArrayList<>());
        
        // 测试无实例情况
        boolean result = selector.hasVersionInstances("test-service", "v1");
        assertFalse(result);
    }

    private ServiceInstance createInstance(String version) {
        ServiceInstance instance = mock(ServiceInstance.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", version);
        when(instance.getMetadata()).thenReturn(metadata);
        when(instance.getUri()).thenReturn(URI.create("http://localhost:8080"));
        return instance;
    }
}
