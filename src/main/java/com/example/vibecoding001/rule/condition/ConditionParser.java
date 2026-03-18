package com.example.vibecoding001.rule.condition;


/**
 * 条件解析器，用于解析条件表达式字符串为条件对象
 * //问题：这里是不是将一个字符串表达式，解析成一个condiction对象？ AI帮我回答一下，我需要一个详细的解析过程
 * //回答：是的，这里将一个字符串表达式解析成一个Condition对象。解析过程如下：
 * //1. 去除表达式的首尾空格
 * //2. 检查表达式是否是一个简单的原子条件（如："age > 18"），如果是，则直接返回一个AtomicCondition对象
 * //3. 检查表达式是否是一个组合条件（如："NOT (age > 18)"），如果是，则递归解析组合条件中的子表达式
 * //4. 如果表达式不是简单条件或组合条件，则抛出异常
 */
public class ConditionParser {

    /**
     * 解析条件表达式字符串
     * @param expression 条件表达式字符串
     * @return 条件对象
     */
    public static Condition parse(String expression) {
        expression = expression.trim();
        return parseExpression(expression);
    }

    /**
     * 递归解析表达式
     * @param expression 表达式字符串
     * @return 条件对象
     */
    private static Condition parseExpression(String expression) {
        if (expression.isEmpty()) {
            return new AtomicCondition("");
        }

        // 处理括号 - 只处理最外层的平衡括号
        if (expression.startsWith("(") && expression.endsWith(")")) {
            // 检查是否是最外层的一对括号
            int parenthesesCount = 0;
            boolean isOuterMostParentheses = true;
            for (int i = 0; i < expression.length(); i++) {
                if (expression.charAt(i) == '(') {
                    parenthesesCount++;
                } else if (expression.charAt(i) == ')') {
                    parenthesesCount--;
                    // 如果在末尾之前括号就平衡了，说明不是最外层括号
                    if (parenthesesCount == 0 && i < expression.length() - 1) {
                        isOuterMostParentheses = false;
                        break;
                    }
                }
            }
            if (isOuterMostParentheses && parenthesesCount == 0) {
                return parseExpression(expression.substring(1, expression.length() - 1));
            }
        }

        // 处理NOT操作符
        if (expression.startsWith("NOT ") || expression.startsWith("not ")) {
            CompositeCondition notCondition = new CompositeCondition(CompositeCondition.Operator.NOT);
            notCondition.addCondition(parseExpression(expression.substring(4).trim()));
            return notCondition;
        }

        // 处理OR操作符（最低优先级）
        int orIndex = findOperatorIndex(expression, "OR");
        if (orIndex != -1) {
            CompositeCondition orCondition = new CompositeCondition(CompositeCondition.Operator.OR);
            orCondition.addCondition(parseExpression(expression.substring(0, orIndex).trim()));
            orCondition.addCondition(parseExpression(expression.substring(orIndex + 2).trim()));
            return orCondition;
        }

        // 处理AND操作符
        int andIndex = findOperatorIndex(expression, "AND");
        if (andIndex != -1) {
            CompositeCondition andCondition = new CompositeCondition(CompositeCondition.Operator.AND);
            andCondition.addCondition(parseExpression(expression.substring(0, andIndex).trim()));
            andCondition.addCondition(parseExpression(expression.substring(andIndex + 3).trim()));
            return andCondition;
        }

        // 原子条件
        return new AtomicCondition(expression);
    }

    /**
     * 查找操作符在表达式中的位置
     * @param expression 表达式字符串
     * @param operator 操作符
     * @return 操作符位置，-1表示未找到
     */
    private static int findOperatorIndex(String expression, String operator) {
        int parenthesesCount = 0;
        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            if (expression.charAt(i) == '(') {
                parenthesesCount++;
            } else if (expression.charAt(i) == ')') {
                parenthesesCount--;
            } else if (parenthesesCount == 0 && expression.substring(i, i + operator.length()).equals(operator)) {
                // 确保是完整的操作符
                boolean isCompleteOperator = true;
                if (i > 0 && Character.isLetterOrDigit(expression.charAt(i - 1))) {
                    isCompleteOperator = false;
                }
                if (i + operator.length() < expression.length() && Character.isLetterOrDigit(expression.charAt(i + operator.length()))) {
                    isCompleteOperator = false;
                }
                if (isCompleteOperator) {
                    return i;
                }
            }
        }
        return -1;
    }
}
