package com.example.payment.payment.enums;

/**
 * 支付状态枚举
 * 定义支付请求在整个生命周期中的各个状态
 */
public enum PaymentStatus {

    /**
     * 待处理：请求已入库，等待聚合批次
     */
    PENDING("PENDING", "待处理"),

    /**
     * 处理中：已提交第三方，等待支付结果
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 支付成功：第三方返回成功或异步通知成功
     */
    SUCCESS("SUCCESS", "支付成功"),

    /**
     * 支付失败：第三方返回业务失败（参数问题等）
     */
    FAILED("FAILED", "支付失败"),

    /**
     * 接口超时：调用第三方接口超时
     */
    TIMEOUT("TIMEOUT", "接口超时"),

    /**
     * 未知异常：系统异常或其他未预期错误
     */
    UNKNOWN_ERROR("UNKNOWN_ERROR", "未知异常");

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举
     */
    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status code: " + code);
    }

    /**
     * 是否为终态（成功或失败）
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == TIMEOUT || this == UNKNOWN_ERROR;
    }

    /**
     * 是否允许重试
     */
    public boolean isRetryable() {
        return this == TIMEOUT || this == UNKNOWN_ERROR;
    }
}
