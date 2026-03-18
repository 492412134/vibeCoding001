package com.example.vibecoding001.rule;

import java.util.Map;

public interface Rule {
    //获取规则ID
    String getId();
    //获取规则名称
    String getName();
    //评估规则是否满足
    boolean evaluate(Map<String, Object> facts);
    //执行规则
    void execute(Map<String, Object> facts);
    //获取规则优先级
    int getPriority();
}
