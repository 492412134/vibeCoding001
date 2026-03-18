package com.example.vibecoding001.rule.external;

import com.example.vibecoding001.entity.ExternalApiConfigEntity;
import com.example.vibecoding001.mapper.ExternalApiConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 外部API控制器
 */
@RestController
@RequestMapping("/api/external")
public class ExternalApiController {

    @Autowired
    private ExternalApiConfigMapper apiConfigMapper;

    @Autowired
    private ExternalApiCaller apiCaller;

    /**
     * 添加API配置
     * @param request API配置请求
     * @return 添加结果
     */
    @PostMapping("/config/add")
    public String addApiConfig(@RequestBody ApiConfigRequest request) {
        String apiId = "api_" + System.currentTimeMillis();
        
        ExternalApiConfigEntity entity = new ExternalApiConfigEntity();
        entity.setApiId(apiId);
        entity.setApiName(request.getApiName());
        entity.setApiUrl(request.getApiUrl());
        entity.setHttpMethod(request.getHttpMethod());
        entity.setRequestTemplate(request.getRequestTemplate());
        entity.setResponseField(request.getResponseField());
        entity.setSuccessCondition(request.getSuccessCondition());
        entity.setHeaders(request.getHeaders());
        entity.setTimeout(request.getTimeout() != null ? request.getTimeout() : 5000);
        entity.setEnabled(true);
        entity.setDescription(request.getDescription());
        
        apiConfigMapper.insert(entity);
        return "API配置添加成功，API ID：" + apiId;
    }

    /**
     * 更新API配置
     * @param request API配置请求
     * @return 更新结果
     */
    @PutMapping("/config/update")
    public String updateApiConfig(@RequestBody ApiConfigRequest request) {
        if (request.getApiId() == null || request.getApiId().isEmpty()) {
            return "API ID不能为空";
        }
        
        ExternalApiConfigEntity entity = new ExternalApiConfigEntity();
        entity.setApiId(request.getApiId());
        entity.setApiName(request.getApiName());
        entity.setApiUrl(request.getApiUrl());
        entity.setHttpMethod(request.getHttpMethod());
        entity.setRequestTemplate(request.getRequestTemplate());
        entity.setResponseField(request.getResponseField());
        entity.setSuccessCondition(request.getSuccessCondition());
        entity.setHeaders(request.getHeaders());
        entity.setTimeout(request.getTimeout());
        entity.setEnabled(request.getEnabled());
        entity.setDescription(request.getDescription());
        
        apiConfigMapper.update(entity);
        return "API配置更新成功";
    }

    /**
     * 删除API配置
     * @param apiId API ID
     * @return 删除结果
     */
    @DeleteMapping("/config/remove/{apiId}")
    public String removeApiConfig(@PathVariable String apiId) {
        apiConfigMapper.deleteById(apiId);
        return "API配置删除成功";
    }

    /**
     * 获取所有API配置
     * @return API配置列表
     */
    @GetMapping("/config/list")
    public List<ExternalApiConfigEntity> listApiConfigs() {
        return apiConfigMapper.selectAll();
    }

    /**
     * 获取启用的API配置
     * @return API配置列表
     */
    @GetMapping("/config/list/enabled")
    public List<ExternalApiConfigEntity> listEnabledApiConfigs() {
        return apiConfigMapper.selectAllEnabled();
    }

    /**
     * 切换API启用/停用状态
     * @param apiId API ID
     * @param enabled 是否启用
     * @return 切换结果
     */
    @PostMapping("/config/toggle/{apiId}")
    public String toggleApiStatus(@PathVariable String apiId, @RequestParam boolean enabled) {
        apiConfigMapper.updateEnabledStatus(apiId, enabled);
        return enabled ? "API已启用" : "API已停用";
    }

    /**
     * 测试API调用
     * @param apiId API ID
     * @param testData 测试数据
     * @return API调用结果
     */
    @PostMapping("/test/{apiId}")
    public ExternalApiResult testApi(@PathVariable String apiId, @RequestBody Map<String, Object> testData) {
        ExternalApiConfigEntity entity = apiConfigMapper.selectById(apiId);
        if (entity == null) {
            ExternalApiResult result = new ExternalApiResult();
            result.setSuccess(false);
            result.setErrorMessage("API配置不存在");
            return result;
        }

        ExternalApiConfig config = convertToConfig(entity);
        return apiCaller.callApi(config, testData);
    }

    /**
     * 将实体转换为配置对象
     */
    private ExternalApiConfig convertToConfig(ExternalApiConfigEntity entity) {
        ExternalApiConfig config = new ExternalApiConfig();
        config.setApiId(entity.getApiId());
        config.setApiName(entity.getApiName());
        config.setApiUrl(entity.getApiUrl());
        config.setHttpMethod(entity.getHttpMethod());
        config.setRequestTemplate(entity.getRequestTemplate());
        config.setResponseField(entity.getResponseField());
        config.setSuccessCondition(entity.getSuccessCondition());
        config.setHeaders(entity.getHeaders());
        config.setTimeout(entity.getTimeout());
        config.setEnabled(entity.getEnabled());
        config.setDescription(entity.getDescription());
        return config;
    }

    /**
     * API配置请求对象
     */
    public static class ApiConfigRequest {
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
}
