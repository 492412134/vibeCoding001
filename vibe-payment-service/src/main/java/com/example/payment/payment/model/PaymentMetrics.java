package com.example.payment.payment.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 支付指标统计
 * 用于监控支付系统的各项性能指标
 */
public class PaymentMetrics {

    // 基础计数器
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong timeoutRequests = new AtomicLong(0);
    private final AtomicLong totalAmount = new AtomicLong(0);
    private final AtomicInteger currentQueueSize = new AtomicInteger(0);
    private final AtomicLong totalBatches = new AtomicLong(0);

    // QPS统计
    private volatile long lastSecondRequests = 0;
    private volatile long lastTimestamp = System.currentTimeMillis();

    // 处理耗时统计（毫秒）
    private final LongAdder totalProcessTime = new LongAdder();      // 总处理时间
    private final AtomicLong maxProcessTime = new AtomicLong(0);     // 最大处理时间
    private final AtomicLong minProcessTime = new AtomicLong(Long.MAX_VALUE); // 最小处理时间
    private final AtomicLong processCount = new AtomicLong(0);       // 处理次数

    // 第三方接口调用统计
    private final LongAdder totalThirdPartyTime = new LongAdder();   // 第三方接口总耗时
    private final AtomicLong thirdPartyCallCount = new AtomicLong(0); // 第三方接口调用次数
    private final AtomicLong thirdPartyTimeoutCount = new AtomicLong(0); // 第三方接口超时次数

    // 队列等待时间统计
    private final LongAdder totalQueueWaitTime = new LongAdder();    // 队列总等待时间
    private final AtomicLong queueWaitCount = new AtomicLong(0);     // 队列等待次数

    // 单例模式
    private static final PaymentMetrics INSTANCE = new PaymentMetrics();

    private PaymentMetrics() {}

    public static PaymentMetrics getInstance() {
        return INSTANCE;
    }

    // ========== 基础记录方法 ==========

    public void recordRequest() {
        totalRequests.incrementAndGet();
    }

    public void recordSuccess() {
        successRequests.incrementAndGet();
    }

    public void recordFailure() {
        failedRequests.incrementAndGet();
    }

    public void recordTimeout() {
        timeoutRequests.incrementAndGet();
        failedRequests.incrementAndGet();
    }

    public void recordAmount(long amount) {
        totalAmount.addAndGet(amount);
    }

    public void recordBatch() {
        totalBatches.incrementAndGet();
    }

    public void updateQueueSize(int size) {
        currentQueueSize.set(size);
    }

    public void recordSecondRequests(long count) {
        this.lastSecondRequests = count;
        this.lastTimestamp = System.currentTimeMillis();
    }

    // ========== 处理耗时记录 ==========

    /**
     * 记录处理耗时
     * @param processTime 处理耗时（毫秒）
     */
    public void recordProcessTime(long processTime) {
        totalProcessTime.add(processTime);
        processCount.incrementAndGet();

        // 更新最大耗时
        long currentMax;
        do {
            currentMax = maxProcessTime.get();
            if (processTime <= currentMax) break;
        } while (!maxProcessTime.compareAndSet(currentMax, processTime));

        // 更新最小耗时
        long currentMin;
        do {
            currentMin = minProcessTime.get();
            if (processTime >= currentMin) break;
        } while (!minProcessTime.compareAndSet(currentMin, processTime));
    }

    /**
     * 记录第三方接口调用耗时
     * @param callTime 调用耗时（毫秒）
     */
    public void recordThirdPartyCallTime(long callTime) {
        totalThirdPartyTime.add(callTime);
        thirdPartyCallCount.incrementAndGet();
    }

    /**
     * 记录第三方接口超时
     */
    public void recordThirdPartyTimeout() {
        thirdPartyTimeoutCount.incrementAndGet();
    }

    /**
     * 记录队列等待时间
     * @param waitTime 等待时间（毫秒）
     */
    public void recordQueueWaitTime(long waitTime) {
        totalQueueWaitTime.add(waitTime);
        queueWaitCount.incrementAndGet();
    }

    // ========== 计算指标方法 ==========

    /**
     * 获取平均处理耗时（毫秒）
     */
    public double getAvgProcessTime() {
        long count = processCount.get();
        if (count == 0) return 0.0;
        return totalProcessTime.sum() / (double) count;
    }

    /**
     * 获取最大处理耗时（毫秒）
     */
    public long getMaxProcessTime() {
        return maxProcessTime.get();
    }

    /**
     * 获取最小处理耗时（毫秒）
     */
    public long getMinProcessTime() {
        long min = minProcessTime.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * 获取平均第三方接口调用耗时（毫秒）
     */
    public double getAvgThirdPartyTime() {
        long count = thirdPartyCallCount.get();
        if (count == 0) return 0.0;
        return totalThirdPartyTime.sum() / (double) count;
    }

    /**
     * 获取第三方接口超时率
     */
    public double getThirdPartyTimeoutRate() {
        long count = thirdPartyCallCount.get();
        if (count == 0) return 0.0;
        return (thirdPartyTimeoutCount.get() * 100.0) / count;
    }

    /**
     * 获取平均队列等待时间（毫秒）
     */
    public double getAvgQueueWaitTime() {
        long count = queueWaitCount.get();
        if (count == 0) return 0.0;
        return totalQueueWaitTime.sum() / (double) count;
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (successRequests.get() * 100.0) / total;
    }

    /**
     * 获取处理吞吐量（笔/秒）
     * 基于总处理时间和处理次数计算
     */
    public double getProcessThroughput() {
        long totalTime = totalProcessTime.sum();
        if (totalTime == 0) return 0.0;
        return (processCount.get() * 1000.0) / totalTime;
    }

    /**
     * 获取第三方接口TPS（每秒事务数）
     * TPS = 调用次数 / 总耗时（秒）
     */
    public double getThirdPartyTps() {
        long totalTime = totalThirdPartyTime.sum();
        if (totalTime == 0) return 0.0;
        return (thirdPartyCallCount.get() * 1000.0) / totalTime;
    }

    // ========== Getters ==========

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessRequests() {
        return successRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public long getTimeoutRequests() {
        return timeoutRequests.get();
    }

    public long getTotalAmount() {
        return totalAmount.get();
    }

    public int getCurrentQueueSize() {
        return currentQueueSize.get();
    }

    public long getTotalBatches() {
        return totalBatches.get();
    }

    public long getLastSecondRequests() {
        return lastSecondRequests;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public long getProcessCount() {
        return processCount.get();
    }

    public long getThirdPartyCallCount() {
        return thirdPartyCallCount.get();
    }

    public long getThirdPartyTimeoutCount() {
        return thirdPartyTimeoutCount.get();
    }

    public long getQueueWaitCount() {
        return queueWaitCount.get();
    }

    public void reset() {
        totalRequests.set(0);
        successRequests.set(0);
        failedRequests.set(0);
        timeoutRequests.set(0);
        totalAmount.set(0);
        currentQueueSize.set(0);
        totalBatches.set(0);
        lastSecondRequests = 0;

        totalProcessTime.reset();
        maxProcessTime.set(0);
        minProcessTime.set(Long.MAX_VALUE);
        processCount.set(0);

        totalThirdPartyTime.reset();
        thirdPartyCallCount.set(0);
        thirdPartyTimeoutCount.set(0);

        totalQueueWaitTime.reset();
        queueWaitCount.set(0);
    }
}
