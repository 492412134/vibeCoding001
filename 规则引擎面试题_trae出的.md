# 规则引擎面试题

基于 vibeCoding001 项目中的规则引擎实现整理的面试题

---

## 题目一：条件解析器递归算法

**看你代码中的 ConditionParser，请解释这段代码：**

```java
private static int findOperatorIndex(String expression, String operator) {
    int parenthesesCount = 0;
    for (int i = 0; i <= expression.length() - operator.length(); i++) {
        if (expression.charAt(i) == '(') {
            parenthesesCount++;
        } else if (expression.charAt(i) == ')') {
            parenthesesCount--;
        } else if (parenthesesCount == 0 && 
                   expression.substring(i, i + operator.length()).equals(operator)) {
            return i;
        }
    }
    return -1;
}
```

### 问题：
1. 这个方法的目的是什么？
2. `parenthesesCount` 的作用是什么？为什么要判断它等于0？
3. 如果输入 `"(age > 18 OR gender == 'male') AND vip == true"` 查找 "OR"，返回的索引是多少？为什么？

### 参考答案：

1. **目的**：在表达式中查找指定操作符的位置，但**跳过括号内的操作符**（因为括号内的操作符优先级更高，应该由递归处理）

2. **parenthesesCount 作用**：
   - 记录当前遍历到的括号深度
   - 只有 `parenthesesCount == 0` 时才在顶层查找操作符
   - 示例：`(A OR B) AND C` 中，OR 在括号内（depth=1），AND 在括号外（depth=0）

3. **返回索引**：查找 "OR" 返回 `-1`（找不到）
   - 因为 OR 在括号内部，parenthesesCount 不为 0
   - 查找 "AND" 返回括号后的索引

---

## 题目二：组合模式的应用

**看你代码中的 CompositeCondition 类：**

```java
public class CompositeCondition implements Condition {
    private Operator operator;  // AND, OR, NOT
    private List<Condition> conditions;
    
    @Override
    public boolean evaluate(Map<String, Object> facts) {
        switch (operator) {
            case AND:
                return conditions.stream().allMatch(c -> c.evaluate(facts));
            case OR:
                return conditions.stream().anyMatch(c -> c.evaluate(facts));
            case NOT:
                return conditions.size() > 0 && !conditions.get(0).evaluate(facts);
        }
    }
}
```

### 问题：
1. 这里使用了什么设计模式？有什么好处？
2. 如果要实现 `XOR`（异或）运算，代码应该怎么改？
3. 当前 `NOT` 的实现只取第一个条件，这样设计合理吗？如果用户写了 `NOT (A AND B)` 会发生什么？

### 参考答案：

1. **设计模式**：组合模式（Composite Pattern）
   - 好处：统一处理原子条件和组合条件，支持无限嵌套

2. **XOR 实现**：
```java
case XOR:
    return conditions.stream()
        .filter(c -> c.evaluate(facts))
        .count() == 1;
```

3. **NOT 设计问题**：
   - 当前实现：`NOT (A AND B)` 只会对 A 取反，B 被忽略
   - 正确做法：NOT 应该只有一个子条件，这个子条件可以是复合条件
   - 应该报错或限制 NOT 只能有一个子条件

---

## 题目三：外部API规则的实现

**看你代码中的这段逻辑：**

```java
// 检查是否是外部API规则
if (ruleEntity != null && ruleEntity.getCondition() != null &&
    ruleEntity.getCondition().startsWith("EXTERNAL_API:")) {
    evaluated = executeExternalApiRule(ruleEntity.getCondition(), facts);
} else {
    evaluated = rule.evaluate(facts);
}
```

### 问题：
1. 这种通过字符串前缀判断规则类型的方式有什么优缺点？
2. 如果以后要支持更多规则类型（如数据库规则、MQ规则），怎么重构更优雅？
3. `executeExternalApiRule` 方法将结果存回 `facts` 中，这种设计的目的是什么？有什么风险？

### 参考答案：

1. **字符串前缀判断优缺点**：
   - 优点：简单、直观
   - 缺点：扩展性差，容易出错，不符合开闭原则

2. **重构方案**（使用策略模式）：
```java
public interface RuleTypeHandler {
    boolean supports(String condition);
    boolean execute(String condition, Map<String, Object> facts);
}

@Component
public class ExternalApiRuleHandler implements RuleTypeHandler {
    public boolean supports(String condition) {
        return condition.startsWith("EXTERNAL_API:");
    }
    // ...
}
```

3. **facts 回写目的和风险**：
   - 目的：让后续规则能看到 API 调用结果，实现规则链
   - 风险：key 可能冲突（`apiResult_` + apiId），污染 facts 数据

---

## 题目四：规则引擎的并发问题

**看你代码中的规则执行：**

```java
public void executeRules(Map<String, Object> facts) {
    loadRulesFromDatabase();  // 每次执行都重新加载
    for (Rule rule : rules) {
        // ... 执行规则
    }
}
```

### 问题：
1. 每次执行都调用 `loadRulesFromDatabase()` 有什么问题？
2. 如果去掉这行，又会出现什么问题？
3. 如果要实现"不重启服务更新规则"，你有什么方案？

### 参考答案：

1. **每次都加载的问题**：
   - 性能差，数据库压力大
   - 执行规则变成同步阻塞操作

2. **去掉的问题**：
   - 规则更新后内存中的规则不会刷新
   - 需要重启服务才能生效

3. **热更新方案**：
```java
// 方案：定时刷新 + 手动刷新
@Scheduled(fixedDelay = 60000)  // 每分钟刷新
public void refreshRules() {
    loadRulesFromDatabase();
}

// 或者使用事件监听
@EventListener
public void onRuleChange(RuleChangeEvent event) {
    loadRulesFromDatabase();
}
```

---

## 题目五：版本控制实现

**看你代码中的 `updateRule` 方法：**

```java
public void updateRule(Rule rule, String versionComment) {
    RuleEntity oldEntity = ruleMapper.selectById(rule.getId());
    if (oldEntity != null) {
        // 1. 保存历史记录
        RuleHistoryEntity history = new RuleHistoryEntity();
        history.setRuleId(oldEntity.getId());
        history.setVersion(oldEntity.getVersion());
        // ... 设置其他字段
        ruleMapper.insertHistory(history);
        
        // 2. 更新规则，版本号+1
        entity.setVersion(oldEntity.getVersion() + 1);
        ruleMapper.update(entity);
    }
}
```

### 问题：
1. 这段代码有没有并发问题？如果有，怎么解决？
2. 如果更新数据库成功，但更新内存 `rules` 列表失败，会出现什么问题？
3. 回滚功能 `rollbackToVersion` 是怎么实现的？有什么限制？

### 参考答案：

1. **并发问题**：
   - 问题：两个线程同时更新同一规则，版本号可能重复
   - 解决：数据库加唯一索引 `(rule_id, version)`，或乐观锁

2. **数据库成功但内存失败**：
   - 问题：数据库和内存不一致，规则引擎行为异常
   - 解决：先更新内存，再更新数据库；或事务回滚

3. **回滚实现**：
   - 从历史表查出指定版本数据，覆盖当前规则
   - 限制：不能回滚到已删除的规则，回滚后版本号继续递增

---

## 面试要点总结

| 考察点 | 回答要点 |
|--------|----------|
| **递归算法** | 括号匹配、操作符优先级、递归下降解析 |
| **设计模式** | 组合模式统一处理原子/复合条件、策略模式扩展规则类型 |
| **并发处理** | CopyOnWriteArrayList适用场景、数据库和内存一致性 |
| **扩展性** | 字符串判断的弊端、开闭原则、插件化设计 |
| **版本控制** | 乐观锁、事务一致性、回滚实现 |

