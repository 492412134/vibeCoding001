package com.example.payment.payment.service;

import com.example.payment.payment.enums.PaymentStatus;
import com.example.payment.payment.model.PaymentRequest;
import com.example.payment.payment.model.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 第三方支付服务
 * 模拟调用第三方批量支付接口
 *
 * 接口行为：
 * 1. 正常情况：返回ACCEPTED，表示请求已接受，等待异步通知
 * 2. 接口超时：调用超时，需要重试
 * 3. 业务失败：参数错误等，直接标记为FAILED
 */
@Service
public class ThirdPartyPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyPaymentService.class);

    private final Random random = new Random();

    // 模拟接口延迟（毫秒）
    private static final int MIN_DELAY_MS = 50;
    private static final int MAX_DELAY_MS = 200;

    // 模拟超时率
    private static final double TIMEOUT_RATE = 0.02; // 2%超时率

    // 模拟业务失败率
    private static final double BUSINESS_FAILURE_RATE = 0.05; // 5%业务失败率

    /**
     * 批量支付接口
     * 注意：第三方接口只返回ACCEPTED，不会直接返回SUCCESS/FAILED
     * 最终结果通过异步通知获取
     *
     * @param requests 支付请求列表（请求对象中已包含batchId）
     * @return 支付结果
     */
    public PaymentResult batchPay(List<PaymentRequest> requests) {
        // 使用请求中的batchId（由Aggregator统一生成）
        String batchId = requests.isEmpty() ? "" : requests.get(0).getBatchId();
        logger.info("Calling third-party batch payment API, batchId: {}, size: {}", batchId, requests.size());

        PaymentResult result = new PaymentResult();
        result.setBatchId(batchId);
        result.setProcessedRequests(requests);
        result.setTotalCount(requests.size());

        try {
            // 模拟接口调用延迟
            simulateDelay();

            // 模拟接口超时
            if (random.nextDouble() < TIMEOUT_RATE) {
                logger.warn("Third-party API timeout, batchId: {}", batchId);
                result.setSuccess(false);
                result.setStatus(PaymentStatus.TIMEOUT);
                result.setMessage("Third-party API timeout");

                // 所有请求标记为超时
                for (PaymentRequest request : requests) {
                    request.setStatus(PaymentStatus.TIMEOUT.getCode());
                    request.setErrorMsg("Third-party API timeout");
                }
                return result;
            }

            // 模拟业务失败（参数问题等）
            if (random.nextDouble() < BUSINESS_FAILURE_RATE) {
                logger.warn("Third-party API business failure, batchId: {}", batchId);
                result.setSuccess(false);
                result.setStatus(PaymentStatus.FAILED);
                result.setMessage("Third-party API business failure: invalid parameters");

                // 所有请求标记为失败
                for (PaymentRequest request : requests) {
                    request.setStatus(PaymentStatus.FAILED.getCode());
                    request.setErrorMsg("Business validation failed: invalid bankcard");
                    request.setProcessTime(LocalDateTime.now());
                }
                return result;
            }

            // 正常情况：返回ACCEPTED，表示已接受，等待异步通知
            logger.info("Third-party API accepted, batchId: {}, waiting for async notification", batchId);
            result.setSuccess(true);
            result.setStatus(PaymentStatus.PROCESSING);
            result.setMessage("Request accepted, waiting for async notification");

            // 请求状态保持PROCESSING，等待异步通知
            for (PaymentRequest request : requests) {
                request.setStatus(PaymentStatus.PROCESSING.getCode());
            }

        } catch (Exception e) {
            logger.error("Third-party API call exception, batchId: {}", batchId, e);
            result.setSuccess(false);
            result.setStatus(PaymentStatus.UNKNOWN_ERROR);
            result.setMessage("Third-party API exception: " + e.getMessage());

            for (PaymentRequest request : requests) {
                request.setStatus(PaymentStatus.UNKNOWN_ERROR.getCode());
                request.setErrorMsg(e.getMessage());
            }
        }

        return result;
    }

    /**
     * 单笔支付接口（备用）
     */
    public PaymentResult singlePay(PaymentRequest request) {
        String batchId = UUID.randomUUID().toString().replace("-", "");
        logger.info("Calling third-party single payment API, batchId: {}, requestId: {}",
                batchId, request.getRequestId());

        PaymentResult result = new PaymentResult();
        result.setBatchId(batchId);
        result.setTotalCount(1);

        try {
            simulateDelay();

            // 模拟超时
            if (random.nextDouble() < TIMEOUT_RATE) {
                logger.warn("Third-party API timeout, batchId: {}", batchId);
                request.setStatus(PaymentStatus.TIMEOUT.getCode());
                request.setErrorMsg("Third-party API timeout");
                result.setSuccess(false);
                result.setStatus(PaymentStatus.TIMEOUT);
                result.setMessage("Third-party API timeout");
                return result;
            }

            // 模拟业务失败
            if (random.nextDouble() < BUSINESS_FAILURE_RATE) {
                logger.warn("Third-party API business failure, batchId: {}", batchId);
                request.setStatus(PaymentStatus.FAILED.getCode());
                request.setErrorMsg("Business validation failed");
                request.setProcessTime(LocalDateTime.now());
                result.setSuccess(false);
                result.setStatus(PaymentStatus.FAILED);
                result.setMessage("Business validation failed");
                return result;
            }

            // 正常情况：返回ACCEPTED
            logger.info("Third-party API accepted, batchId: {}", batchId);
            request.setStatus(PaymentStatus.PROCESSING.getCode());
            result.setSuccess(true);
            result.setStatus(PaymentStatus.PROCESSING);
            result.setMessage("Request accepted, waiting for async notification");

        } catch (Exception e) {
            logger.error("Third-party API call exception, batchId: {}", batchId, e);
            request.setStatus(PaymentStatus.UNKNOWN_ERROR.getCode());
            request.setErrorMsg(e.getMessage());
            result.setSuccess(false);
            result.setStatus(PaymentStatus.UNKNOWN_ERROR);
            result.setMessage("Third-party API exception: " + e.getMessage());
        }

        return result;
    }

    /**
     * 模拟异步通知
     * 第三方系统在支付完成后回调此接口
     */
    public PaymentResult simulateAsyncNotification(List<PaymentRequest> requests, String batchId) {
        logger.info("Simulating async notification, batchId: {}, size: {}", batchId, requests.size());

        PaymentResult result = new PaymentResult();
        result.setBatchId(batchId);
        result.setTotalCount(requests.size());

        int successCount = 0;
        int failCount = 0;

        for (PaymentRequest request : requests) {
            // 模拟异步通知结果（大部分成功，少量失败）
            if (random.nextDouble() < 0.02) { // 2%异步通知失败
                request.setStatus(PaymentStatus.FAILED.getCode());
                request.setErrorMsg("Payment failed: insufficient balance");
                request.setProcessTime(LocalDateTime.now());
                failCount++;
            } else {
                request.setStatus(PaymentStatus.SUCCESS.getCode());
                request.setProcessTime(LocalDateTime.now());
                successCount++;
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setSuccess(failCount == 0);
        result.setStatus(failCount == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        result.setMessage(String.format("Async notification: %d success, %d failed", successCount, failCount));

        logger.info("Async notification completed, batchId: {}, success: {}, failed: {}",
                batchId, successCount, failCount);

        return result;
    }

    /**
     * 模拟接口延迟
     */
    private void simulateDelay() {
        int delay = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS);
        try {
            // 打个日志，用中文显示接口延迟多久
            logger.info("Simulating third-party API 模拟第三方支付接口延迟 delay, delay: {} ms", delay);

        
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
