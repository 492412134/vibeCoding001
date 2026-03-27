package com.example.payment.rule.condition;

import com.example.payment.rule.RuleEvaluator;
import java.util.Map;

/**
 * 原子条件类，表示一个基本的条件表达式
 */
public class AtomicCondition implements Condition {
    private String expression;

    /**
     * 构造函数
     * @param expression 条件表达式
     */
    public AtomicCondition(String expression) {
        this.expression = expression;
    }

    /**
     * 评估条件是否满足
     * @param facts 事实数据
     * @return 是否满足条件
     */
    @Override
    public boolean evaluate(Map<String, Object> facts) {
        try {
            return RuleEvaluator.evaluate(expression, facts);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将条件转换为表达式字符串
     * @return 表达式字符串
     */
    @Override
    public String toExpression() {
        return expression;
    }
}
