package com.example.payment.payment.model;

import com.example.payment.payment.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量支付结果
 */
public class PaymentResult {

    private String batchId;
    private boolean success;
    private PaymentStatus status;
    private String message;
    private List<PaymentRequest> processedRequests;
    private LocalDateTime processTime;
    private int totalCount;
    private int successCount;
    private int failCount;

    public PaymentResult() {
        this.processTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PaymentRequest> getProcessedRequests() {
        return processedRequests;
    }

    public void setProcessedRequests(List<PaymentRequest> processedRequests) {
        this.processedRequests = processedRequests;
    }

    public LocalDateTime getProcessTime() {
        return processTime;
    }

    public void setProcessTime(LocalDateTime processTime) {
        this.processTime = processTime;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }
}
