package com.example.vibecoding001.rule;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则评估器，用于评估规则条件和执行规则动作
 */
public class RuleEvaluator {
    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    
    // 用于提取变量名的正则表达式
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * 评估规则表达式是否满足
     * @param condition 规则表达式
     * @param facts 事实数据
     * @return 是否满足规则表达式
     * @throws ScriptException 脚本执行异常
     */
    public static boolean evaluate(String condition, Map<String, Object> facts) throws ScriptException {
        // 验证条件表达式格式
        if (!isValidCondition(condition)) {
            throw new ScriptException("条件表达式格式错误：括号不匹配或语法错误 - " + condition);
        }
        
        // 设置事实数据到引擎
        for (Map.Entry<String, Object> entry : facts.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }
        
        // 自动初始化条件中使用的但未在facts中定义的变量为null
        initializeUndefinedVariables(condition, facts);
        
        Object result = engine.eval(condition);
        return Boolean.parseBoolean(result.toString());
    }

    /**
     * 验证条件表达式格式是否正确
     * @param condition 条件表达式
     * @return 是否有效
     */
    private static boolean isValidCondition(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }
        
        // 检查括号匹配
        int parenthesesCount = 0;
        for (char c : condition.toCharArray()) {
            if (c == '(') {
                parenthesesCount++;
            } else if (c == ')') {
                parenthesesCount--;
                if (parenthesesCount < 0) {
                    return false; // 右括号多于左括号
                }
            }
        }
        
        return parenthesesCount == 0; // 括号必须完全匹配
    }
    
    /**
     * 初始化条件表达式中使用但未在facts中定义的变量为null
     * @param condition 条件表达式
     * @param facts 事实数据
     */
    private static void initializeUndefinedVariables(String condition, Map<String, Object> facts) {
        Matcher matcher = VARIABLE_PATTERN.matcher(condition);
        Set<String> processedVars = new HashSet<>();
        
        while (matcher.find()) {
            String varName = matcher.group();
            
            // 跳过JavaScript关键字和已处理的变量
            if (isJavaScriptKeyword(varName) || processedVars.contains(varName)) {
                continue;
            }
            
            // 如果变量不在facts中，初始化为null
            if (!facts.containsKey(varName)) {
                engine.put(varName, null);
            }
            
            processedVars.add(varName);
        }
    }
    
    /**
     * 检查是否为JavaScript关键字或保留字
     * @param word 单词
     * @return 是否为关键字
     */
    private static boolean isJavaScriptKeyword(String word) {
        Set<String> keywords = Set.of(
            "break", "case", "catch", "continue", "debugger", "default", "delete", "do", "else",
            "finally", "for", "function", "if", "in", "instanceof", "new", "return", "switch",
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "class", "const",
            "enum", "export", "extends", "import", "super", "implements", "interface", "let",
            "package", "private", "protected", "public", "static", "yield", "true", "false", "null", "undefined"
        );
        return keywords.contains(word.toLowerCase());
    }

    /**
     * 执行规则动作
     * @param action 动作代码
     * @param facts 事实数据
     * @throws ScriptException 脚本执行异常
     */
    public static void execute(String action, Map<String, Object> facts) throws ScriptException {
        // 存储原始键集合
        Set<String> originalKeys = new HashSet<>(facts.keySet());
        
        // 设置变量到引擎
        for (Map.Entry<String, Object> entry : facts.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
        }
        
        // 执行动作
        engine.eval(action);
        
        // 更新原始变量
        for (String key : originalKeys) {
            Object value = engine.get(key);
            if (value != null) {
                facts.put(key, value);
            }
        }
        
        // 提取新创建的变量
        try {
            // 尝试获取所有变量
            javax.script.Bindings bindings = engine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE);
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                String key = entry.getKey();
                // 排除系统变量和原始变量
                if (!key.startsWith("javax") && !key.startsWith("com") && !originalKeys.contains(key)) {
                    facts.put(key, entry.getValue());
                }
            }
        } catch (Exception e) {
            // 忽略绑定获取失败的情况
        }
    }
}
