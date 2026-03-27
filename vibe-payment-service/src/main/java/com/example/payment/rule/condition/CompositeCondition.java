package com.example.payment.rule.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 复合条件类，支持AND、OR、NOT逻辑组合
 */
public class CompositeCondition implements Condition {
    /**
     * 逻辑运算符枚举
     */
    public enum Operator {
        AND, OR, NOT
    }

    private Operator operator;
    private List<Condition> conditions;

    /**
     * 构造函数
     * @param operator 逻辑运算符
     */
    public CompositeCondition(Operator operator) {
        this.operator = operator;
        this.conditions = new ArrayList<>();
    }

    /**
     * 构造函数
     * @param operator 逻辑运算符
     * @param conditions 子条件列表
     */
    public CompositeCondition(Operator operator, Condition... conditions) {
        this.operator = operator;
        this.conditions = new ArrayList<>();
        for (Condition condition : conditions) {
            this.conditions.add(condition);
        }
    }

    /**
     * 添加子条件
     * @param condition 子条件
     */
    public void addCondition(Condition condition) {
        conditions.add(condition);
    }

    /**
     * 评估条件是否满足
     * @param facts 事实数据
     * @return 是否满足条件
     */
    @Override
    public boolean evaluate(Map<String, Object> facts) {
        switch (operator) {
            case AND:
                return conditions.stream().allMatch(condition -> condition.evaluate(facts));
            case OR:
                return conditions.stream().anyMatch(condition -> condition.evaluate(facts));
            case NOT:
                return conditions.size() > 0 && !conditions.get(0).evaluate(facts);
            default:
                return false;
        }
    }

    /**
     * 将条件转换为表达式字符串
     * @return 表达式字符串
     */
    @Override
    public String toExpression() {
        if (conditions.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        if (operator == Operator.NOT) {
            sb.append("!(");
            sb.append(conditions.get(0).toExpression());
            sb.append(")");
        } else {
            sb.append("(");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sb.append(" ").append(operator.name()).append(" ");
                }
                sb.append(conditions.get(i).toExpression());
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * 获取逻辑运算符
     * @return 逻辑运算符
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * 获取子条件列表
     * @return 子条件列表
     */
    public List<Condition> getConditions() {
        return conditions;
    }
}
