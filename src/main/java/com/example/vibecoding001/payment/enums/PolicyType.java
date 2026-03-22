package com.example.vibecoding001.payment.enums;

/**
 * 政策类型枚举
 * 用于雪花算法中的4位政策类型标识
 */
public enum PolicyType {
    COMMON(0, "common", "普通政策"),
    OLD_MAN(1, "old_man", "老人政策"),
    EDU(2, "edu", "教育政策"),
    LIVE(3, "live", "生活政策");

    private final int code;
    private final String name;
    private final String description;

    PolicyType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据名称获取枚举
     */
    public static PolicyType fromName(String name) {
        for (PolicyType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return COMMON;
    }

    /**
     * 根据code获取枚举
     */
    public static PolicyType fromCode(int code) {
        for (PolicyType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return COMMON;
    }

    /**
     * 获取4位二进制字符串
     */
    public String getBinaryString() {
        return String.format("%4s", Integer.toBinaryString(code)).replace(' ', '0');
    }
}
