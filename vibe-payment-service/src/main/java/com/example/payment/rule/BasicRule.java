package com.example.payment.rule;

import com.example.payment.rule.condition.Condition;
import com.example.payment.rule.condition.ConditionParser;
import java.util.Map;

public class BasicRule implements Rule {
    private String id;
    private String name;
    private String condition;
    private String action;
    private int priority;

    public BasicRule(String id, String name, String condition, String action, int priority) {
        this.id = id;
        this.name = name;
        this.condition = condition;
        this.action = action;
        this.priority = priority;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean evaluate(Map<String, Object> facts) {
        try {
            Condition parsedCondition = ConditionParser.parse(condition);
            return parsedCondition.evaluate(facts);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void execute(Map<String, Object> facts) {
        try {
            RuleEvaluator.execute(action, facts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public String getCondition() {
        return condition;
    }

    public String getAction() {
        return action;
    }
}
