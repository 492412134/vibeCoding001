package com.example.payment.payment.enums;

/**
 * 订单类型枚举
 */
public enum OrderType {
    VIP("VIP", "VIP订单", 1),
    NORMAL("NORMAL", "普通订单", 2);

    private final String code;
    private final String description;
    private final int priority; // 优先级，数字越小优先级越高

    OrderType(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public static OrderType fromCode(String code) {
        for (OrderType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return NORMAL; // 默认为普通订单
    }
}
