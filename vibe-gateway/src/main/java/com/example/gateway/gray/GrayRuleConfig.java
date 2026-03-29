package com.example.gateway.gray;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
//@Component
//@RefreshScope
@ConfigurationProperties(prefix = "gray")
public class GrayRuleConfig {
    private List<GrayRule> rules;

    @Data
    public static class GrayRule {
        private String service;
        private boolean enabled;
        private int weight;
        private List<GrayCondition> conditions;
        private String version;
    }

    @Data
    public static class GrayCondition {
        private String type;
        private String operator;
        private List<String> values;
    }
}
