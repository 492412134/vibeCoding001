package com.example.payment.rule.condition;

import java.util.Map;

/**
 * 条件接口，定义条件的评估和表达式转换方法
 */
public interface Condition {
    /**
     * 评估条件是否满足
     * @param facts 事实数据
     * @return 是否满足条件
     */
    boolean evaluate(Map<String, Object> facts);
    
    /**
     * 将条件转换为表达式字符串
     * @return 表达式字符串
     */
    String toExpression();
}
