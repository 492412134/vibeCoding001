package com.example.vibecoding001.rule.external;

/**
 * 外部API配置类
 */
public class ExternalApiConfig {
    
    private String apiId;
    private String apiName;
    private String apiUrl;
    private String httpMethod;
    private String requestTemplate;
    private String responseField;
    private String successCondition;
    private String headers;
    private Integer timeout;
    private Boolean enabled;
    private String description;

    public ExternalApiConfig() {
        this.timeout = 5000;
        this.enabled = true;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getRequestTemplate() {
        return requestTemplate;
    }

    public void setRequestTemplate(String requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    public String getResponseField() {
        return responseField;
    }

    public void setResponseField(String responseField) {
        this.responseField = responseField;
    }

    public String getSuccessCondition() {
        return successCondition;
    }

    public void setSuccessCondition(String successCondition) {
        this.successCondition = successCondition;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
