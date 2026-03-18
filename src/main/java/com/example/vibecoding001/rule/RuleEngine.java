package com.example.vibecoding001.rule;

import com.example.vibecoding001.entity.ExternalApiConfigEntity;
import com.example.vibecoding001.entity.RuleEntity;
import com.example.vibecoding001.entity.RuleExecutionLog;
import com.example.vibecoding001.entity.RuleHistoryEntity;
import com.example.vibecoding001.mapper.ExternalApiConfigMapper;
import com.example.vibecoding001.mapper.RuleExecutionLogMapper;
import com.example.vibecoding001.mapper.RuleMapper;
import com.example.vibecoding001.rule.external.ExternalApiCaller;
import com.example.vibecoding001.rule.external.ExternalApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 规则引擎核心类，管理规则的加载、执行和状态切换
 */
@Component
public class RuleEngine {
    private static final List<Rule> rules = new CopyOnWriteArrayList<>();

    @Autowired
    private RuleMapper ruleMapper;

    @Autowired
    private RuleExecutionLogMapper executionLogMapper;

    @Autowired
    private ExternalApiConfigMapper externalApiConfigMapper;

    @Autowired
    private ExternalApiCaller externalApiCaller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化方法，加载启用的规则到内存
     */
    @PostConstruct
    public void init() {
        loadRulesFromDatabase();
    }

    /**
     * 从数据库加载启用的规则
     */
    private void loadRulesFromDatabase() {
        rules.clear();
        List<RuleEntity> ruleEntities = ruleMapper.selectAllEnabled();
        for (RuleEntity entity : ruleEntities) {
            Rule rule = new BasicRule(
                entity.getId(),
                entity.getName(),
                entity.getCondition(),
                entity.getAction(),
                entity.getPriority()
            );
            rules.add(rule);
        }
        sortRules();
    }

    /**
     * 添加规则
     * @param rule 规则对象
     */
    public void addRule(Rule rule) {
        RuleEntity entity = new RuleEntity(
            rule.getId(),
            rule.getName(),
            ((BasicRule) rule).getCondition(),
            ((BasicRule) rule).getAction(),
            rule.getPriority()
        );
        entity.setEnabled(true);
        entity.setVersion(1);
        entity.setVersionComment("初始版本");
        ruleMapper.insert(entity);

        RuleHistoryEntity history = new RuleHistoryEntity();
        history.setRuleId(entity.getId());
        history.setVersion(1);
        history.setName(entity.getName());
        history.setCondition(entity.getCondition());
        history.setAction(entity.getAction());
        history.setPriority(entity.getPriority());
        history.setEnabled(entity.getEnabled());
        history.setVersionComment("初始版本");
        history.setCreatedBy("system");
        ruleMapper.insertHistory(history);

        rules.add(rule);
        sortRules();
    }

    /**
     * 添加外部API规则
     * @param rule 规则对象
     * @param apiId 关联的API配置ID
     */
    public void addExternalApiRule(Rule rule, String apiId) {
        ExternalApiConfigEntity apiConfig = externalApiConfigMapper.selectById(apiId);
        if (apiConfig == null) {
            throw new RuntimeException("API配置不存在：" + apiId);
        }

        // 保存规则基本信息
        RuleEntity entity = new RuleEntity();
        entity.setId(rule.getId());
        entity.setName(rule.getName());
        entity.setCondition("EXTERNAL_API:" + apiId);
        entity.setAction("调用外部API: " + apiConfig.getApiName());
        entity.setPriority(rule.getPriority());
        entity.setEnabled(true);
        entity.setVersion(1);
        entity.setVersionComment("外部API规则 - 初始版本");
        ruleMapper.insert(entity);

        // 保存历史记录
        RuleHistoryEntity history = new RuleHistoryEntity();
        history.setRuleId(entity.getId());
        history.setVersion(1);
        history.setName(entity.getName());
        history.setCondition(entity.getCondition());
        history.setAction(entity.getAction());
        history.setPriority(entity.getPriority());
        history.setEnabled(entity.getEnabled());
        history.setVersionComment("外部API规则 - 初始版本");
        history.setCreatedBy("system");
        ruleMapper.insertHistory(history);

        rules.add(rule);
        sortRules();
    }

    /**
     * 删除规则
     * @param ruleId 规则ID
     */
    public void removeRule(String ruleId) {
        ruleMapper.deleteById(ruleId);
        rules.removeIf(rule -> rule.getId().equals(ruleId));
    }

    /**
     * 更新规则
     * @param rule 规则对象
     * @param versionComment 版本注释
     */
    public void updateRule(Rule rule, String versionComment) {
        RuleEntity oldEntity = ruleMapper.selectById(rule.getId());
        RuleEntity entity = null;

        if (oldEntity != null) {
            RuleHistoryEntity history = new RuleHistoryEntity();
            history.setRuleId(oldEntity.getId());
            history.setVersion(oldEntity.getVersion());
            history.setName(oldEntity.getName());
            history.setCondition(oldEntity.getCondition());
            history.setAction(oldEntity.getAction());
            history.setPriority(oldEntity.getPriority());
            history.setEnabled(oldEntity.getEnabled());
            history.setVersionComment(oldEntity.getVersionComment());
            history.setCreatedBy("system");
            ruleMapper.insertHistory(history);

            entity = new RuleEntity(
                rule.getId(),
                rule.getName(),
                ((BasicRule) rule).getCondition(),
                ((BasicRule) rule).getAction(),
                rule.getPriority()
            );
            entity.setEnabled(true);
            entity.setVersion(oldEntity.getVersion() + 1);
            entity.setVersionComment(versionComment != null ? versionComment : "更新版本");
            ruleMapper.update(entity);
        }

        if (entity != null) {
            // 从内存中移除旧规则，然后添加新规则
            rules.removeIf(r -> r.getId().equals(rule.getId()));
            rules.add(rule);
            sortRules();
        }
    }

    /**
     * 切换规则启用/停用状态
     * @param ruleId 规则ID
     * @param enabled 是否启用
     */
    public void toggleRuleStatus(String ruleId, boolean enabled) {
        ruleMapper.updateEnabledStatus(ruleId, enabled);
        if (enabled) {
            // 启用规则，从数据库加载到内存
            RuleEntity entity = ruleMapper.selectById(ruleId);
            if (entity != null) {
                Rule rule = new BasicRule(
                    entity.getId(),
                    entity.getName(),
                    entity.getCondition(),
                    entity.getAction(),
                    entity.getPriority()
                );
                rules.add(rule);
                sortRules();
            }
        } else {
            // 停用规则，从内存中移除
            rules.removeIf(rule -> rule.getId().equals(ruleId));
        }
    }

    /**
     * 获取启用的规则列表
     * @return 规则列表
     */
    public List<Rule> getRules() {
        return new ArrayList<>(rules);
    }

    /**
     * 获取所有规则列表（包括停用）
     * @return 规则实体列表
     */
    public List<RuleEntity> getAllRuleEntities() {
        return ruleMapper.selectAll();
    }

    /**
     * 获取规则历史记录
     * @param ruleId 规则ID
     * @return 历史记录列表
     */
    public List<RuleHistoryEntity> getRuleHistory(String ruleId) {
        return ruleMapper.selectHistoryByRuleId(ruleId);
    }

    /**
     * 回滚规则到指定版本
     * @param ruleId 规则ID
     * @param version 版本号
     */
    public void rollbackToVersion(String ruleId, Integer version) {
        RuleHistoryEntity history = ruleMapper.selectHistoryByVersion(ruleId, version);
        if (history != null) {
            RuleEntity oldEntity = ruleMapper.selectById(ruleId);
            if (oldEntity != null) {
                RuleHistoryEntity currentHistory = new RuleHistoryEntity();
                currentHistory.setRuleId(oldEntity.getId());
                currentHistory.setVersion(oldEntity.getVersion());
                currentHistory.setName(oldEntity.getName());
                currentHistory.setCondition(oldEntity.getCondition());
                currentHistory.setAction(oldEntity.getAction());
                currentHistory.setPriority(oldEntity.getPriority());
                currentHistory.setEnabled(oldEntity.getEnabled());
                currentHistory.setVersionComment(oldEntity.getVersionComment());
                currentHistory.setCreatedBy("system");
                ruleMapper.insertHistory(currentHistory);
            }

            ruleMapper.rollbackToVersion(ruleId, version);
            loadRulesFromDatabase();
        }
    }

    /**
     * 执行规则并记录执行日志
     * @param facts 事实数据
     */
    public void executeRules(Map<String, Object> facts) {
        loadRulesFromDatabase();
        String inputData = convertToJson(facts);

        for (Rule rule : rules) {
            long startTime = System.currentTimeMillis();
            RuleExecutionLog log = new RuleExecutionLog();
            log.setRuleId(rule.getId());
            log.setRuleName(rule.getName());

            // 获取规则详细信息
            RuleEntity ruleEntity = ruleMapper.selectById(rule.getId());
            if (ruleEntity != null) {
                log.setCondition(ruleEntity.getCondition());
                log.setAction(ruleEntity.getAction());
            }

            log.setInputData(inputData);

            try {
                boolean evaluated;

                // 检查是否是外部API规则
                if (ruleEntity != null && ruleEntity.getCondition() != null &&
                    ruleEntity.getCondition().startsWith("EXTERNAL_API:")) {
                    // 外部API规则
                    evaluated = executeExternalApiRule(ruleEntity.getCondition(), facts);
                } else {
                    // 普通规则
                    evaluated = rule.evaluate(facts);
                }

                if (evaluated) {
                    rule.execute(facts);
                    log.setExecuted(true);
                } else {
                    log.setExecuted(false);
                }
                log.setOutputData(convertToJson(facts));
            } catch (Exception e) {
                log.setExecuted(false);
                log.setErrorMessage(e.getMessage());
            }

            long endTime = System.currentTimeMillis();
            log.setExecutionTime(endTime - startTime);

            // 保存执行日志
            executionLogMapper.insert(log);
        }
    }

    /**
     * 执行外部API规则
     * @param condition 条件字符串，格式：EXTERNAL_API:apiId
     * @param facts 事实数据
     * @return 是否通过
     */
    private boolean executeExternalApiRule(String condition, Map<String, Object> facts) {
        String apiId = condition.substring("EXTERNAL_API:".length());
        ExternalApiConfigEntity apiConfigEntity = externalApiConfigMapper.selectById(apiId);

        if (apiConfigEntity == null || !apiConfigEntity.getEnabled()) {
            return false;
        }

        ExternalApiConfig config = convertToConfig(apiConfigEntity);
        var result = externalApiCaller.callApi(config, facts);

        // 将API结果存入facts
        facts.put("apiResult_" + apiId, result);
        facts.put("apiSuccess_" + apiId, result.getSuccess());

        return result.getSuccess() != null && result.getSuccess();
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
     * 将对象转换为JSON字符串
     * @param obj 对象
     * @return JSON字符串
     */
    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj != null ? obj.toString() : null;
        }
    }

    /**
     * 对规则按优先级排序
     */
    private void sortRules() {
        rules.sort(Comparator.comparingInt(Rule::getPriority).reversed());
    }

    /**
     * 获取规则引擎单例实例
     * @return 规则引擎实例
     */
    public static RuleEngine getInstance() {
        return RuleEngineHolder.INSTANCE;
    }

    private static class RuleEngineHolder {
        private static final RuleEngine INSTANCE = new RuleEngine();
    }
}
