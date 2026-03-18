# 规则引擎文档

> 本文档详细说明规则引擎的功能、架构、配置和使用方法。

## 1. 服务概述

### 1.1 功能简介
规则引擎是一个灵活的业务规则管理系统，支持：
- 复杂条件表达式解析
- 规则版本控制与回滚
- 规则执行监控与日志
- 外部API集成

### 1.2 核心特性
| 特性 | 说明 |
|------|------|
| 条件组合 | 支持 AND/OR/NOT 逻辑组合 |
| 版本控制 | 规则版本管理，支持历史回滚 |
| 执行监控 | 详细记录规则执行过程 |
| 外部API | 支持调用外部HTTP接口 |

## 2. 核心功能

### 2.1 条件表达式系统

#### 2.1.1 原子条件
```java
public class AtomicCondition implements Condition {
    private String field;      // 字段名
    private String operator;   // 操作符
    private Object value;      // 比较值
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        Object fieldValue = context.get(field);
        return compare(fieldValue, operator, value);
    }
}
```

#### 2.1.2 支持的操作符
| 操作符 | 说明 | 示例 |
|--------|------|------|
| `==` | 等于 | `age == 18` |
| `!=` | 不等于 | `status != "deleted"` |
| `>` | 大于 | `score > 60` |
| `<` | 小于 | `price < 100` |
| `>=` | 大于等于 | `level >= 5` |
| `<=` | 小于等于 | `count <= 10` |
| `contains` | 包含 | `name contains "张"` |

#### 2.1.3 组合条件
```java
public class CompositeCondition implements Condition {
    private String logic;                    // AND/OR/NOT
    private List<Condition> conditions;      // 子条件列表
    
    @Override
    public boolean evaluate(Map<String, Object> context) {
        switch (logic) {
            case "AND":
                return conditions.stream().allMatch(c -> c.evaluate(context));
            case "OR":
                return conditions.stream().anyMatch(c -> c.evaluate(context));
            case "NOT":
                return !conditions.get(0).evaluate(context);
        }
    }
}
```

### 2.2 规则定义

#### 2.2.1 规则结构
```java
public class Rule {
    private String ruleId;           // 规则ID
    private String name;             // 规则名称
    private String condition;        // 条件表达式（JSON格式）
    private String action;           // 动作代码
    private int priority;            // 优先级
    private boolean enabled;         // 是否启用
    private int version;             // 版本号
    private String versionComment;   // 版本说明
}
```

#### 2.2.2 条件表达式示例
```json
{
  "type": "composite",
  "logic": "AND",
  "conditions": [
    {
      "type": "atomic",
      "field": "age",
      "operator": ">=",
      "value": 18
    },
    {
      "type": "atomic",
      "field": "status",
      "operator": "==",
      "value": "active"
    }
  ]
}
```

#### 2.2.3 动作代码示例
```java
// 动作代码支持Groovy脚本
return [
    result: "APPROVED",
    message: "审核通过",
    discount: context.amount * 0.1
];
```

### 2.3 版本控制

#### 2.3.1 版本管理机制
```
创建规则 → 版本=1
    │
    ▼
修改规则 → 版本+1，旧版本保存到history表
    │
    ▼
可回滚到任意历史版本
```

#### 2.3.2 版本相关API
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rule/history/{ruleId}` | GET | 获取规则历史 |
| `/api/rule/rollback/{ruleId}/{version}` | POST | 回滚到指定版本 |

#### 2.3.3 历史记录表
```sql
CREATE TABLE rule_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id VARCHAR(50) NOT NULL,
    version INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    condition TEXT NOT NULL,
    action TEXT NOT NULL,
    priority INT DEFAULT 0,
    enabled TINYINT(1) DEFAULT 1,
    version_comment VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2.4 执行监控

#### 2.4.1 执行日志
```java
public class RuleExecutionLog {
    private Long id;
    private String ruleId;           // 规则ID
    private String ruleName;         // 规则名称
    private Integer ruleVersion;     // 规则版本
    private String inputData;        // 输入数据
    private String outputData;       // 输出结果
    private Boolean matched;         // 是否匹配
    private Long executionTime;      // 执行耗时
    private Timestamp executedAt;    // 执行时间
}
```

#### 2.4.2 监控指标
| 指标 | 说明 |
|------|------|
| 执行次数 | 规则执行总次数 |
| 匹配率 | 匹配成功次数/总次数 |
| 平均耗时 | 规则执行平均时间 |
| 失败率 | 执行失败次数/总次数 |

## 3. 核心类说明

### 3.1 RuleEngine
**位置**：`com.example.vibecoding001.rule.RuleEngine`

**核心方法**：
| 方法 | 说明 |
|------|------|
| `evaluate()` | 评估规则，返回匹配结果 |
| `evaluateAll()` | 评估所有规则，返回所有匹配结果 |
| `addRule()` | 动态添加规则 |
| `removeRule()` | 移除规则 |

### 3.2 RuleEvaluator
**位置**：`com.example.vibecoding001.rule.RuleEvaluator`

**职责**：
- 解析条件表达式
- 执行条件评估
- 调用动作代码

### 3.3 ConditionParser
**位置**：`com.example.vibecoding001.rule.condition.ConditionParser`

**功能**：
- 将JSON条件解析为Condition对象
- 支持嵌套条件解析

### 3.4 RuleController
**位置**：`com.example.vibecoding001.rule.RuleController`

**API接口**：
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rule/evaluate` | POST | 执行规则评估 |
| `/api/rule/list` | GET | 获取规则列表 |
| `/api/rule/add` | POST | 添加规则 |
| `/api/rule/update` | PUT | 更新规则 |
| `/api/rule/delete/{ruleId}` | DELETE | 删除规则 |

## 4. 外部API集成

### 4.1 外部API配置
```java
public class ExternalApiConfig {
    private String apiId;            // API标识
    private String apiName;          // API名称
    private String url;              // 请求URL
    private String method;           // HTTP方法
    private String headers;          // 请求头（JSON）
    private String paramMapping;     // 参数映射（JSON）
    private String resultMapping;    // 结果映射（JSON）
    private Integer timeout;         // 超时时间（秒）
    private Integer retryTimes;      // 重试次数
}
```

### 4.2 参数映射
```json
{
  "userId": "${context.userId}",
  "amount": "${context.amount}",
  "timestamp": "${system.currentTimeMillis()}"
}
```

### 4.3 结果映射
```json
{
  "success": "${response.code == 200}",
  "message": "${response.message}",
  "data": "${response.data}"
}
```

## 5. 使用示例

### 5.1 创建规则
```bash
curl -X POST http://localhost:8080/api/rule/add \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "RULE_001",
    "name": "VIP用户折扣规则",
    "condition": "{\"type\":\"atomic\",\"field\":\"userLevel\",\"operator\":\">=\",\"value\":5}",
    "action": "return [result: \"APPROVED\", discount: 0.1]",
    "priority": 10,
    "enabled": true
  }'
```

### 5.2 执行规则评估
```bash
curl -X POST http://localhost:8080/api/rule/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "RULE_001",
    "context": {
      "userLevel": 8,
      "amount": 1000
    }
  }'
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "matched": true,
    "result": {
      "result": "APPROVED",
      "discount": 0.1
    },
    "executionTime": 15
  }
}
```

### 5.3 查看规则历史
```bash
curl http://localhost:8080/api/rule/history/RULE_001
```

### 5.4 回滚规则版本
```bash
curl -X POST http://localhost:8080/api/rule/rollback/RULE_001/2
```

## 6. 配置参数

### 6.1 规则引擎配置
```properties
# 规则缓存大小
rule.engine.cacheSize=1000

# 规则执行超时时间（毫秒）
rule.engine.timeout=5000

# 是否记录执行日志
rule.engine.logEnabled=true
```

### 6.2 外部API配置
```properties
# 默认超时时间（秒）
external.api.defaultTimeout=30

# 默认重试次数
external.api.defaultRetryTimes=3

# 连接池大小
external.api.connectionPoolSize=50
```

## 7. 最佳实践

### 7.1 规则设计原则
1. **单一职责**：每条规则只处理一种业务场景
2. **优先级设置**：重要规则设置较高优先级
3. **条件简化**：避免过于复杂的嵌套条件
4. **版本注释**：每次修改添加版本说明

### 7.2 性能优化
```java
// 1. 启用规则缓存
ruleEngine.enableCache(true);

// 2. 批量评估
List<RuleResult> results = ruleEngine.evaluateAll(context);

// 3. 异步执行
CompletableFuture<RuleResult> future = 
    CompletableFuture.supplyAsync(() -> ruleEngine.evaluate(rule, context));
```

### 7.3 调试技巧
```java
// 开启调试模式
ruleEngine.setDebugMode(true);

// 查看条件解析结果
Condition condition = ConditionParser.parse(conditionJson);
System.out.println(condition.toString());

// 查看执行日志
List<RuleExecutionLog> logs = ruleExecutionLogMapper.findByRuleId("RULE_001");
```

## 8. 故障排查

### 8.1 规则不匹配
**排查步骤**：
1. 检查条件表达式语法
2. 验证输入数据格式
3. 查看操作符是否支持该数据类型
4. 检查字段名是否正确

### 8.2 动作执行失败
**排查步骤**：
1. 检查动作代码语法
2. 验证上下文变量是否存在
3. 查看返回结果格式
4. 检查外部API调用是否成功

### 8.3 性能问题
**优化建议**：
1. 启用规则缓存
2. 减少不必要的规则评估
3. 优化条件表达式复杂度
4. 增加执行超时设置

---

**文档版本**：v1.0  
**最后更新**：2026-03-18  
**维护人员**：VibeCoding Team
