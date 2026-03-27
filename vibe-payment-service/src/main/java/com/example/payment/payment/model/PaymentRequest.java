package com.example.payment.payment.model;

import com.example.payment.payment.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付请求实体
 */
public class PaymentRequest {

    private Long id;
    private Long snowflakeId;
    private String requestId;
    private String name;
    private String idcard;
    private String bankcard;
    private BigDecimal amount;
    private String status;
    private String batchId;
    private String policyType;
    private Long policyCode;
    private LocalDateTime createTime;
    private LocalDateTime submitTime;
    private LocalDateTime processTime;
    private String errorMsg;
    private int retryCount;
    private OrderType orderType; // VIP or NORMAL

    public PaymentRequest() {
        // 注意：无参构造函数不初始化任何字段
        // 因为MyBatis查询时会使用这个构造函数，然后反射设置字段值
        // 如果在这里初始化，会覆盖从数据库查询的值
    }

    /**
     * 创建新的支付请求（业务使用）
     */
    public static PaymentRequest createNew(String name, String idcard, String bankcard, BigDecimal amount) {
        return createNew(name, idcard, bankcard, amount, OrderType.NORMAL);
    }

    /**
     * 创建新的支付请求（带订单类型）
     */
    public static PaymentRequest createNew(String name, String idcard, String bankcard, BigDecimal amount, OrderType orderType) {
        PaymentRequest request = new PaymentRequest();
        request.requestId = UUID.randomUUID().toString().replace("-", "");
        request.createTime = LocalDateTime.now();
        request.status = "PENDING";
        request.retryCount = 0;
        request.name = name;
        request.idcard = idcard;
        request.bankcard = bankcard;
        request.amount = amount;
        request.orderType = orderType != null ? orderType : OrderType.NORMAL;
        return request;
    }
    
    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIdcard() {
        return idcard;
    }
    
    public void setIdcard(String idcard) {
        this.idcard = idcard;
    }
    
    public String getBankcard() {
        return bankcard;
    }
    
    public void setBankcard(String bankcard) {
        this.bankcard = bankcard;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSnowflakeId() {
        return snowflakeId;
    }

    public void setSnowflakeId(Long snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public Long getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(Long policyCode) {
        this.policyCode = policyCode;
    }

    /**
     * 是否为VIP订单
     */
    public boolean isVip() {
        return orderType == OrderType.VIP;
    }
}
