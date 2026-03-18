package com.example.vibecoding001.rule.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外部API调用器
 */
@Component
public class ExternalApiCaller {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");

    public ExternalApiCaller() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 调用外部API并判断结果
     * @param config API配置
     * @param facts 事实数据
     * @return API调用结果
     */
    public ExternalApiResult callApi(ExternalApiConfig config, Map<String, Object> facts) {
        ExternalApiResult result = new ExternalApiResult();
        result.setApiId(config.getApiId());
        result.setApiName(config.getApiName());

        try {
            // 构建请求URL
            String url = buildUrl(config.getApiUrl(), facts);

            // 构建请求头
            Headers headers = buildHeaders(config.getHeaders(), facts);

            // 构建请求体
            String requestBody = buildRequestBody(config.getRequestTemplate(), facts);

            // 构建请求
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .headers(headers);

            // 根据HTTP方法设置请求体
            if (config.getHttpMethod().equalsIgnoreCase("POST")) {
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                requestBuilder.post(RequestBody.create(requestBody, mediaType));
            } else if (config.getHttpMethod().equalsIgnoreCase("PUT")) {
                MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                requestBuilder.put(RequestBody.create(requestBody, mediaType));
            } else if (config.getHttpMethod().equalsIgnoreCase("DELETE")) {
                requestBuilder.delete();
            } else { // GET
                requestBuilder.get();
            }

            // 发送请求
            long startTime = System.currentTimeMillis();
            Response response = okHttpClient.newCall(requestBuilder.build()).execute();
            long endTime = System.currentTimeMillis();

            result.setResponseTime(endTime - startTime);
            result.setHttpStatus(response.code());

            // 解析响应
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                // 将JsonNode转换为Map，避免序列化问题
                Map<String, Object> responseMap = objectMapper.convertValue(responseJson, Map.class);
                result.setResponseData(responseMap);

                // 判断是否成功
                boolean success = evaluateSuccessCondition(config.getSuccessCondition(), config.getResponseField(), responseJson, facts);
                result.setSuccess(success);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("HTTP错误: " + response.code() + " " + response.message());
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(String urlTemplate, Map<String, Object> facts) {
        return replaceVariables(urlTemplate, facts);
    }

    /**
     * 构建请求头
     */
    private Headers buildHeaders(String headersTemplate, Map<String, Object> facts) {
        Headers.Builder builder = new Headers.Builder();
        builder.add("Content-Type", "application/json");

        if (headersTemplate != null && !headersTemplate.isEmpty()) {
            String resolvedHeaders = replaceVariables(headersTemplate, facts);
            try {
                JsonNode headersJson = objectMapper.readTree(resolvedHeaders);
                headersJson.fields().forEachRemaining(entry -> {
                    builder.add(entry.getKey(), entry.getValue().asText());
                });
            } catch (Exception e) {
                // 如果解析失败，忽略自定义头
            }
        }

        return builder.build();
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String template, Map<String, Object> facts) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        return replaceVariables(template, facts);
    }

    /**
     * 评估成功条件
     */
    private boolean evaluateSuccessCondition(String condition, String responseField, JsonNode responseJson, Map<String, Object> facts) {
        try {
            // 提取响应字段值
            Object fieldValue = extractFieldValue(responseJson, responseField);

            // 将变量设置到脚本引擎
            scriptEngine.put("response", fieldValue);
            scriptEngine.put("responseJson", responseJson.toString());

            for (Map.Entry<String, Object> entry : facts.entrySet()) {
                scriptEngine.put(entry.getKey(), entry.getValue());
            }

            // 执行条件表达式
            Object result = scriptEngine.eval(condition);
            return Boolean.parseBoolean(result.toString());

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从JSON中提取字段值
     */
    private Object extractFieldValue(JsonNode jsonNode, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return jsonNode;
        }

        String[] fields = fieldPath.split("\\.");
        JsonNode current = jsonNode;

        for (String field : fields) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(field);
        }

        if (current == null || current.isNull()) {
            return null;
        }

        if (current.isBoolean()) {
            return current.asBoolean();
        } else if (current.isNumber()) {
            return current.asDouble();
        } else {
            return current.asText();
        }
    }

    /**
     * 替换模板中的变量
     */
    private String replaceVariables(String template, Map<String, Object> facts) {
        if (template == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = facts.get(varName);
            if (value != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value.toString()));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
