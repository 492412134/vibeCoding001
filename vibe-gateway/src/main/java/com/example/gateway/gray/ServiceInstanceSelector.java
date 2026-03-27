package com.example.gateway.gray;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServiceInstanceSelector {
    private final DiscoveryClient discoveryClient;
    private final Random random = new Random();

    public ServiceInstance selectInstance(String serviceId, String version) {
        // 获取所有服务实例
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }

        // 过滤出指定版本的实例
        List<ServiceInstance> versionInstances = instances.stream()
                .filter(instance -> isMatchVersion(instance, version))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(versionInstances)) {
            return null;
        }

        // 随机选择一个实例
        return versionInstances.get(random.nextInt(versionInstances.size()));
    }

    public ServiceInstance selectDefaultInstance(String serviceId) {
        // 获取所有服务实例
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (CollectionUtils.isEmpty(instances)) {
            return null;
        }

        // 过滤出非灰度版本的实例（版本为空或为默认版本）
        List<ServiceInstance> defaultInstances = instances.stream()
                .filter(instance -> {
                    String instanceVersion = instance.getMetadata().get("version");
                    return instanceVersion == null || instanceVersion.equals("v1");
                })
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(defaultInstances)) {
            // 如果没有默认版本，返回任意实例
            return instances.get(random.nextInt(instances.size()));
        }

        // 随机选择一个默认实例
        return defaultInstances.get(random.nextInt(defaultInstances.size()));
    }

    private boolean isMatchVersion(ServiceInstance instance, String version) {
        if (instance == null || version == null) {
            return false;
        }

        String instanceVersion = instance.getMetadata().get("version");
        return version.equals(instanceVersion);
    }

    public boolean hasVersionInstances(String serviceId, String version) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (CollectionUtils.isEmpty(instances)) {
            return false;
        }

        return instances.stream()
                .anyMatch(instance -> isMatchVersion(instance, version));
    }
}
