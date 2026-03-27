package com.example.order.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class Order implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private Long id;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 订单状态：PENDING-待支付，PAID-已支付，CANCELLED-已取消
     */
    private String status;
    
    /**
     * 是否VIP订单
     */
    private Boolean vip;
    
    /**
     * 支付请求ID
     */
    private String paymentRequestId;
    
    /**
     * 身份证号（用于支付服务）
     */
    private String idCard;
    
    /**
     * 政策类型
     */
    private String policyType;
    
    /**
     * 政策编号
     */
    private Long policyCode;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
