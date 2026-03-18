package com.example.vibecoding001.rule;

import com.example.vibecoding001.entity.RuleEntity;
import com.example.vibecoding001.entity.RuleExecutionLog;
import com.example.vibecoding001.entity.RuleHistoryEntity;
import com.example.vibecoding001.mapper.RuleExecutionLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 规则控制器，提供规则管理的REST API
 */
@RestController
@RequestMapping("/api/rule")
public class RuleController {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private RuleExecutionLogMapper executionLogMapper;

    /**
     * 添加规则
     * @param ruleRequest 规则请求对象
     * @return 添加结果消息
     */
    @PostMapping("/add")
    public String addRule(@RequestBody RuleRequest ruleRequest) {
        String ruleId = "rule_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);

        // 检查是否是外部API规则
        if (ruleRequest.getCondition() != null && ruleRequest.getCondition().startsWith("EXTERNAL_API:")) {
            String apiId = ruleRequest.getCondition().substring("EXTERNAL_API:".length());
            BasicRule rule = new BasicRule(
                    ruleId,
                    ruleRequest.getName(),
                    ruleRequest.getCondition(),
                    ruleRequest.getAction(),
                    ruleRequest.getPriority()
            );
            ruleEngine.addExternalApiRule(rule, apiId);
            return "外部API规则添加成功，生成的规则ID：" + ruleId;
        }

        BasicRule rule = new BasicRule(
                ruleId,
                ruleRequest.getName(),
                ruleRequest.getCondition(),
                ruleRequest.getAction(),
                ruleRequest.getPriority()
        );
        ruleEngine.addRule(rule);
        return "规则添加成功，生成的规则ID：" + ruleId;
    }

    /**
     * 删除规则
     * @param id 规则ID
     * @return 删除结果消息
     */
    @DeleteMapping("/remove/{id}")
    public String removeRule(@PathVariable String id) {
        ruleEngine.removeRule(id);
        return "规则删除成功";
    }

    /**
     * 更新规则
     * @param ruleRequest 规则请求对象
     * @return 更新结果消息
     */
    @PutMapping("/update")
    public String updateRule(@RequestBody RuleRequest ruleRequest) {
        if (ruleRequest.getId() == null || ruleRequest.getId().isEmpty()) {
            return "规则ID不能为空";
        }
        BasicRule rule = new BasicRule(
                ruleRequest.getId(),
                ruleRequest.getName(),
                ruleRequest.getCondition(),
                ruleRequest.getAction(),
                ruleRequest.getPriority()
        );
        ruleEngine.updateRule(rule, ruleRequest.getVersionComment());
        return "规则更新成功";
    }

    /**
     * 获取启用的规则列表
     * @return 规则列表
     */
    @GetMapping("/list")
    public List<Rule> listRules() {
        return ruleEngine.getRules();
    }

    /**
     * 获取所有规则列表（包括停用）
     * @return 规则实体列表
     */
    @GetMapping("/list/all")
    public List<RuleEntity> listAllRules() {
        return ruleEngine.getAllRuleEntities();
    }

    /**
     * 执行规则
     * @param facts 事实数据
     * @return 执行后的结果
     */
    @PostMapping("/execute")
    public Map<String, Object> executeRules(@RequestBody Map<String, Object> facts) {
        ruleEngine.executeRules(facts);
        return facts;
    }

    /**
     * 获取规则历史记录
     * @param ruleId 规则ID
     * @return 历史记录列表
     */
    @GetMapping("/history/{ruleId}")
    public List<RuleHistoryEntity> getRuleHistory(@PathVariable String ruleId) {
        return ruleEngine.getRuleHistory(ruleId);
    }

    /**
     * 回滚规则到指定版本
     * @param ruleId 规则ID
     * @param version 版本号
     * @return 回滚结果消息
     */
    @PostMapping("/rollback/{ruleId}/{version}")
    public String rollbackToVersion(@PathVariable String ruleId, @PathVariable Integer version) {
        ruleEngine.rollbackToVersion(ruleId, version);
        return "规则已回滚到版本 " + version;
    }

    /**
     * 切换规则启用/停用状态
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @return 切换结果消息
     */
    @PostMapping("/toggle/{ruleId}")
    public String toggleRuleStatus(@PathVariable String ruleId, @RequestParam boolean enabled) {
        ruleEngine.toggleRuleStatus(ruleId, enabled);
        return enabled ? "规则已启用" : "规则已停用";
    }

    // ==================== 规则执行监控接口 ====================

    /**
     * 获取所有执行日志
     * @return 执行日志列表
     */
    @GetMapping("/execution/logs")
    public List<RuleExecutionLog> getAllExecutionLogs() {
        return executionLogMapper.selectAll();
    }

    /**
     * 获取指定规则的执行日志
     * @param ruleId 规则ID
     * @return 执行日志列表
     */
    @GetMapping("/execution/logs/{ruleId}")
    public List<RuleExecutionLog> getExecutionLogsByRuleId(@PathVariable String ruleId) {
        return executionLogMapper.selectByRuleId(ruleId);
    }

    /**
     * 获取最近的执行日志
     * @param limit 限制数量，默认50
     * @return 执行日志列表
     */
    @GetMapping("/execution/logs/recent")
    public List<RuleExecutionLog> getRecentExecutionLogs(@RequestParam(defaultValue = "50") int limit) {
        return executionLogMapper.selectRecent(limit);
    }

    /**
     * 根据时间范围查询执行日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行日志列表
     */
    @GetMapping("/execution/logs/timeRange")
    public List<RuleExecutionLog> getExecutionLogsByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.parse(endTime);
        return executionLogMapper.selectByTimeRange(start, end);
    }

    /**
     * 获取规则的执行统计信息
     * @param ruleId 规则ID
     * @return 统计信息
     */
    @GetMapping("/execution/stats/{ruleId}")
    public ExecutionStats getExecutionStats(@PathVariable String ruleId) {
        long totalCount = executionLogMapper.countByRuleId(ruleId);
        double successRate = executionLogMapper.calculateSuccessRate(ruleId);
        return new ExecutionStats(totalCount, successRate);
    }

    /**
     * 执行统计信息对象
     */
    public static class ExecutionStats {
        private long totalCount;
        private double successRate;

        public ExecutionStats(long totalCount, double successRate) {
            this.totalCount = totalCount;
            this.successRate = successRate;
        }

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(double successRate) {
            this.successRate = successRate;
        }
    }

    /**
     * 规则请求对象
     */
    public static class RuleRequest {
        private String id;
        private String name;
        private String condition;
        private String action;
        private int priority;
        private String versionComment;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public String getVersionComment() {
            return versionComment;
        }

        public void setVersionComment(String versionComment) {
            this.versionComment = versionComment;
        }
    }
}
