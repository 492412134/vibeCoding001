package com.example.payment.rule.external;

import com.example.payment.rule.Rule;

import java.util.Map;

/**
 * 外部API规则，用于调用第三方接口并根据结果判断规则是否通过
 */
public class ExternalApiRule implements Rule {

    private String id;
    private String name;
    private int priority;
    private ExternalApiConfig apiConfig;
    private ExternalApiCaller apiCaller;

    public ExternalApiRule(String id, String name, int priority, ExternalApiConfig apiConfig, ExternalApiCaller apiCaller) {
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.apiConfig = apiConfig;
        this.apiCaller = apiCaller;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean evaluate(Map<String, Object> facts) {
        // 调用外部API并获取结果
        ExternalApiResult result = apiCaller.callApi(apiConfig, facts);
        
        // 将API调用结果存入facts，供后续使用
        facts.put("apiResult_" + apiConfig.getApiId(), result);
        facts.put("apiSuccess_" + apiConfig.getApiId(), result.getSuccess());
        
        // 返回API调用是否成功
        return result.getSuccess() != null && result.getSuccess();
    }

    @Override
    public void execute(Map<String, Object> facts) {
        // 外部API规则通常不需要执行动作，evaluate已经调用了API
        // 如果需要，可以在这里添加额外的处理逻辑
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public ExternalApiConfig getApiConfig() {
        return apiConfig;
    }

    public void setApiConfig(ExternalApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }
}
