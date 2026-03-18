package com.example.vibecoding001.payment.service;

import com.example.vibecoding001.payment.enums.PaymentStatus;
import com.example.vibecoding001.payment.model.PaymentMetrics;
import com.example.vibecoding001.payment.model.PaymentRequest;
import com.example.vibecoding001.payment.model.PaymentResult;
import com.example.vibecoding001.payment.repository.PaymentRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 支付聚合服务
 * 负责聚合支付请求并批量处理
 *
 * 流程：请求入库 -> 内存队列 -> 批量聚合 -> 调用第三方
 *
 * 触发条件（满足任一）：
 * 1. 队列满10条立即处理
 * 2. 等待超过1秒立即处理
 */
@Service
public class PaymentAggregator {

    private static final Logger logger = LoggerFactory.getLogger(PaymentAggregator.class);

    // 批量处理阈值
    private static final int BATCH_SIZE_THRESHOLD = 10;
    private static final long TIME_THRESHOLD_MS = 1000; // 1秒

    @Autowired
    private ThirdPartyPaymentService thirdPartyPaymentService;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    private final PaymentMetrics metrics = PaymentMetrics.getInstance();

    // 批量处理线程池（支持30个批次并行）
    private ThreadPoolExecutor batchProcessor;

    // QPS统计任务调度器
    private ScheduledExecutorService metricsScheduler;

    // 每秒请求数统计
    private final AtomicLong secondRequestCount = new AtomicLong(0);

    // VIP队列：高优先级订单
    private final BlockingQueue<PaymentRequest> vipQueue = new LinkedBlockingQueue<>();

    // 普通队列：普通优先级订单
    private final BlockingQueue<PaymentRequest> normalQueue = new LinkedBlockingQueue<>();

    // 线程状态追踪：记录每个线程当前处理的元素数量和状态   ·
    private final ConcurrentHashMap<String, ThreadStatus> threadStatusMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 恢复未处理的请求（服务重启时）
        recoverPendingRequests();

        // 批量处理线程池：基于CPU核数动态计算
        // IO密集型任务：线程数 = CPU核数 * (1 + 等待时间/计算时间) ≈ CPU核数 * 5
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int coreThreads = Math.max(20, cpuCores * 2);  // 至少20，或CPU核数*2
        // int maxThreads = Math.max(40, cpuCores * 4);   // 至少40，或CPU核数*4
        int maxThreads = coreThreads;   // 至少20，或CPU核数*4

        batchProcessor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    // 为每个线程设置有意义的名称，方便调试和监控，从0开始编号
                    int threadNum = threadStatusMap.size();
                    Thread t = new Thread(r, "payment-batch-processor-" + threadNum);
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // QPS统计调度器
        metricsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "payment-metrics-scheduler");
            t.setDaemon(true);
            return t;
        });

        // 启动每秒统计任务
        metricsScheduler.scheduleAtFixedRate(this::calculateQps, 1, 1, TimeUnit.SECONDS);

        // 启动批量收集线程
        startBatchCollectorThread();

        logger.info("PaymentAggregator initialized, cpuCores: {}, threadPool: {}/{}", cpuCores, coreThreads, maxThreads);
    }

    /**
     * 服务重启时恢复未处理的请求
     * 使用Set去重，防止重复恢复同一请求
     */
    private void recoverPendingRequests() {
        try {
            // 使用Set记录已恢复的requestId，防止重复
            java.util.Set<String> recoveredIds = new java.util.HashSet<>();

            // 如果等待处理的请求超过1000条，分批恢复
            int offset = 0;
            int batchSize = 100;
            int totalRecovered = 0;
            int totalPendingInDb = 0;

            while (totalRecovered < 1000) {
                List<PaymentRequest> pendingList = paymentRequestRepository.findPendingRequestsWithOffset(batchSize, offset);
                if (pendingList.isEmpty()) {
                    break;
                }

                totalPendingInDb += pendingList.size();

                for (PaymentRequest request : pendingList) {
                    // 去重检查
                    if (!recoveredIds.contains(request.getRequestId())) {
                        recoveredIds.add(request.getRequestId());
                        // 根据订单类型放入对应队列
                        if (request.isVip()) {
                            vipQueue.offer(request);
                        } else {
                            normalQueue.offer(request);
                        }
                        totalRecovered++;
                        logger.debug("Recovered request: {}, status={}, batchId={}, orderType={}",
                                request.getRequestId(), request.getStatus(), request.getBatchId(),
                                request.getOrderType() != null ? request.getOrderType().getCode() : "NORMAL");
                    } else {
                        logger.warn("Duplicate request found during recovery: {}", request.getRequestId());
                    }
                }

                offset += batchSize;

                // 如果本次查询不足batchSize，说明没有更多数据了
                if (pendingList.size() < batchSize) {
                    break;
                }
            }

            logger.info("Recovered {} pending requests from database (total {} PENDING in DB)",
                    totalRecovered, totalPendingInDb);
        } catch (Exception e) {
            logger.error("Failed to recover pending requests", e);
        }
    }

    /**
     * 启动批量收集线程
     * 使用 BlockingQueue.poll(timeout) 实现：满10条立即处理，或1秒超时处理
     */
    private void startBatchCollectorThread() {
        Thread collectorThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<PaymentRequest> batch = collectBatch();
                    if (!batch.isEmpty()) {
                        processBatch(batch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in batch collector thread", e);
                }
            }
        }, "payment-batch-collector");
        collectorThread.setDaemon(true);
        collectorThread.start();
    }

    /**
     * 收集一批数据
     * 等待策略：满10条立即返回，或1秒超时返回
     * 优先处理VIP队列，VIP队列为空时才处理普通队列
     */
    private List<PaymentRequest> collectBatch() throws InterruptedException {
        List<PaymentRequest> batch = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (batch.size() < BATCH_SIZE_THRESHOLD) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = TIME_THRESHOLD_MS - elapsed;

            if (remaining <= 0) {
                // 已超时，退出收集
                logger.debug("Batch collection timeout, collected {} requests", batch.size());
                break;
            }

            // 优先从VIP队列取数据
            PaymentRequest request = vipQueue.poll();
            boolean isVip = true;

            // VIP队列为空时，从普通队列取数据
            if (request == null) {
                request = normalQueue.poll(remaining, TimeUnit.MILLISECONDS);
                isVip = false;
            }

            if (request != null) {
                batch.add(request);
                logger.debug("Added to batch: {}/{}, isVip={}, waited {}ms",
                        batch.size(), BATCH_SIZE_THRESHOLD, isVip,
                        System.currentTimeMillis() - startTime);

                // 满10条立即处理，不继续等待
                if (batch.size() >= BATCH_SIZE_THRESHOLD) {
                    logger.debug("Batch size reached threshold: {}", batch.size());
                    break;
                }
            } else {
                // poll返回null，说明超时了
                logger.debug("Poll timeout, collected {} requests", batch.size());
                break;
            }
        }

        return batch;
    }

    /**
     * 接收单笔支付请求
     * 流程：先入库（持久化），再入队（快速处理）
     */
    public void submitPayment(PaymentRequest request) {
        // 0. 确保请求对象有必要的字段（如果是从API接收的，可能缺少这些字段）
        if (request.getRequestId() == null || request.getRequestId().isEmpty()) {
            request.setRequestId(java.util.UUID.randomUUID().toString().replace("-", ""));
            logger.debug("Generated new requestId: {}", request.getRequestId());
        }
        if (request.getCreateTime() == null) {
            request.setCreateTime(java.time.LocalDateTime.now());
        }
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            request.setStatus(PaymentStatus.PENDING.getCode());
        }
        if (request.getRetryCount() == 0) {
            request.setRetryCount(0);
        }
        if (request.getOrderType() == null) {
            request.setOrderType(com.example.vibecoding001.payment.enums.OrderType.NORMAL);
        }

        // 1. 先入库（保证数据不丢失）
        // 注意：不使用@Transactional，确保insert立即提交，避免事务延迟导致的问题
        try {
            int inserted = paymentRequestRepository.insert(request);
            if (inserted <= 0) {
                logger.error("Insert returned 0 rows, requestId: {}", request.getRequestId());
                throw new RuntimeException("Failed to insert payment request");
            }
            logger.info("Payment request saved to database: {}, rows={}", request.getRequestId(), inserted);

            // 立即验证插入是否成功
            PaymentRequest verify = paymentRequestRepository.findById(request.getRequestId());
            if (verify == null) {
                logger.error("Verification failed: request not found after insert, requestId: {}", request.getRequestId());
                throw new RuntimeException("Failed to verify payment request insertion");
            }
            logger.debug("Verification passed: request found in database, requestId: {}", request.getRequestId());
        } catch (Exception e) {
            logger.error("Failed to save payment request: {}", request.getRequestId(), e);
            throw new RuntimeException("Failed to save payment request", e);
        }

        // 2. 再入内存队列（快速处理）
        // 根据订单类型放入对应队列
        boolean isVip = request.isVip();
        if (isVip) {
            vipQueue.offer(request);
        } else {
            normalQueue.offer(request);
        }

        // 记录指标
        metrics.recordRequest();
        secondRequestCount.incrementAndGet();
        metrics.updateQueueSize(getTotalQueueSize());

        logger.debug("Payment request submitted: {}, isVip={}, vipQueue={}, normalQueue={}",
                request.getRequestId(), isVip, vipQueue.size(), normalQueue.size());
    }

    /**
     * 获取总队列大小
     */
    private int getTotalQueueSize() {
        return vipQueue.size() + normalQueue.size();
    }

    /**
     * 处理批量支付请求
     * 状态流转：PENDING -> [调用第三方] -> PROCESSING(第三方已接受) -> SUCCESS/FAILED(异步通知)
     *                    -> FAILED(业务失败)
     *                    -> TIMEOUT(接口超时)
     *
     * 注意：PROCESSING状态是在调用第三方接口成功后才更新，表示"已提交给第三方，等待异步通知"
     * 
     * batch_id 生成规则：
     * 1. PENDING状态的请求在数据库中batch_id为null
     * 2. 调用第三方时生成统一的batch_id
     * 3. 更新为PROCESSING时，将batch_id写入数据库
     */
    private void processBatch(List<PaymentRequest> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        // 记录批次开始处理时间（用于计算队列等待时间）
        long batchStartTime = System.currentTimeMillis();

        // 步骤1：筛选出有效的PENDING状态请求
        List<PaymentRequest> pendingRequests = filterPendingRequests(batch, batchStartTime);

        if (pendingRequests.isEmpty()) {
            logger.warn("No valid PENDING requests to process in batch");
            return;
        }

        // 步骤2：生成统一的批次ID（用于这一批次所有请求）
        String batchId = java.util.UUID.randomUUID().toString().replace("-", "");
        logger.info("Processing batch payment, batchId: {}, size: {}", batchId, pendingRequests.size());

        // 将batchId设置到每个请求（内存中，还未写入数据库）
        for (PaymentRequest request : pendingRequests) {
            request.setBatchId(batchId);
        }

        // 步骤3：提交到线程池异步调用第三方
        submitBatchToProcessor(pendingRequests, batchId, batchStartTime);
    }

    /**
     * 筛选出PENDING状态的请求
     * 只检查内存状态，数据库状态在更新时检查（CAS）
     * @return PENDING状态的请求列表
     */
    private List<PaymentRequest> filterPendingRequests(List<PaymentRequest> batch, long batchStartTime) {
        List<PaymentRequest> pendingRequests = new ArrayList<>();

        for (PaymentRequest request : batch) {
            try {
                String requestId = request.getRequestId();

                // 检查内存状态
                String currentStatus = request.getStatus();

                // 如果已经是PROCESSING/SUCCESS/FAILED/TIMEOUT，跳过
                if (PaymentStatus.PROCESSING.getCode().equals(currentStatus) ||
                    PaymentStatus.SUCCESS.getCode().equals(currentStatus) ||
                    PaymentStatus.FAILED.getCode().equals(currentStatus) ||
                    PaymentStatus.TIMEOUT.getCode().equals(currentStatus)) {
                    logger.debug("Request already processed (status={}), skip: {}", currentStatus, requestId);
                    continue;
                }

                // 只有PENDING状态才能继续
                if (!PaymentStatus.PENDING.getCode().equals(currentStatus)) {
                    logger.warn("Request status is not PENDING, skip: {}, status: {}", requestId, currentStatus);
                    continue;
                }

                // 记录队列等待时间
                recordQueueWaitTime(request, batchStartTime);

                pendingRequests.add(request);
                logger.debug("Request validated as PENDING, will call third party: {}", requestId);

            } catch (Exception e) {
                logger.error("Failed to filter request: {}", request.getRequestId(), e);
            }
        }

        return pendingRequests;
    }

    /**
     * 记录队列等待时间
     */
    private void recordQueueWaitTime(PaymentRequest request, long batchStartTime) {
        if (request.getCreateTime() == null) {
            return;
        }

        try {
            // 修复：使用toEpochMilli()获取毫秒时间戳，而非getNano()
            long createTimeMillis = request.getCreateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                    + request.getCreateTime().getNano() / 1_000_000;
            long queueWaitTime = batchStartTime - createTimeMillis;

            // 确保等待时间为正数（防止时钟回拨导致的负数）
            if (queueWaitTime > 0) {
                metrics.recordQueueWaitTime(queueWaitTime);
            }
        } catch (Exception e) {
            logger.warn("Failed to record queue wait time for request: {}", request.getRequestId(), e);
        }
    }

    /**
     * 提交批次到处理器
     */
    private void submitBatchToProcessor(List<PaymentRequest> pendingRequests, String batchId, long batchStartTime) {
        batchProcessor.submit(() -> {
            String threadName = Thread.currentThread().getName();
            long processStartTime = System.currentTimeMillis();

            // 记录线程开始处理状态
            threadStatusMap.put(threadName, new ThreadStatus(threadName, pendingRequests.size(), true, processStartTime, batchId));

            try {
                // 循环调用第三方单个支付接口
                for (PaymentRequest request : pendingRequests) {
                    try {
                        PaymentResult result = thirdPartyPaymentService.singlePay(request);

                        // 记录单个请求的第三方接口调用耗时
                        long singleRequestTime = System.currentTimeMillis() - processStartTime;
                        metrics.recordThirdPartyCallTime(singleRequestTime);

                        // 处理单个请求的结果
                        handleSingleRequestResult(request, result);

                    } catch (Exception e) {
                        logger.error("Error processing single payment request, requestId: {}", request.getRequestId(), e);
                        handleSingleRequestException(request, e);
                    }
                }

                // 更新指标
                metrics.recordBatch();

                // 记录总处理耗时
                long totalProcessTime = System.currentTimeMillis() - batchStartTime;
                metrics.recordProcessTime(totalProcessTime);

                logger.info("Batch processing completed, batchId: {}, size: {}", batchId, pendingRequests.size());

            } catch (Exception e) {
                logger.error("Error processing batch payment, batchId: {}", batchId, e);
                handleBatchException(pendingRequests, batchId, e);
            } finally {
                // 记录线程处理完成状态
                threadStatusMap.put(threadName, new ThreadStatus(threadName, 0, false, 0, null));
            }
        });
    }

    /**
     * 处理单个请求的结果
     */
    private void handleSingleRequestResult(PaymentRequest request, PaymentResult result) {
        PaymentStatus status = result.getStatus();

        if (status == null) {
            logger.error("Single request result status is null, requestId: {}", request.getRequestId());
            return;
        }

        switch (status) {
            case PROCESSING:
                markRequestProcessing(request, result.getBatchId());
                break;

            case SUCCESS:
                markRequestSuccess(request);
                metrics.recordSuccess();
                break;

            case FAILED:
                markRequestFailed(request, result.getMessage());
                metrics.recordFailure();
                break;

            case TIMEOUT:
                markRequestTimeout(request, result.getMessage());
                metrics.recordTimeout();
                metrics.recordThirdPartyTimeout();
                break;

            default:
                logger.warn("Single request unknown status: {}, requestId: {}", status, request.getRequestId());
                break;
        }
    }

    /**
     * 处理单个请求的异常
     */
    private void handleSingleRequestException(PaymentRequest request, Exception e) {
        metrics.recordFailure();
        logger.error("Single request exception, requestId: {}", request.getRequestId(), e);
    }

    /**
     * 标记单个请求为PROCESSING状态
     */
    private void markRequestProcessing(PaymentRequest request, String batchId) {
        try {
            int updated = paymentRequestRepository.updateToProcessing(request.getRequestId(), batchId);
            if (updated > 0) {
                request.setStatus(PaymentStatus.PROCESSING.getCode());
                logger.debug("Marked request as PROCESSING: {}", request.getRequestId());
            } else {
                logger.warn("Failed to mark request as PROCESSING (not PENDING in DB): {}", request.getRequestId());
            }
        } catch (Exception e) {
            logger.error("Failed to mark request as PROCESSING: {}", request.getRequestId(), e);
        }
    }

    /**
     * 标记单个请求为SUCCESS状态
     */
    private void markRequestSuccess(PaymentRequest request) {
        try {
            int updated = paymentRequestRepository.updateToSuccess(request.getRequestId());
            if (updated > 0) {
                request.setStatus(PaymentStatus.SUCCESS.getCode());
                logger.debug("Marked request as SUCCESS: {}", request.getRequestId());
            } else {
                logger.warn("Failed to mark request as SUCCESS (0 rows updated): {}", request.getRequestId());
            }
        } catch (Exception e) {
            logger.error("Failed to mark request as SUCCESS: {}", request.getRequestId(), e);
        }
    }

    /**
     * 标记单个请求为FAILED状态
     */
    private void markRequestFailed(PaymentRequest request, String errorMsg) {
        try {
            int updated = paymentRequestRepository.updateToFailed(request.getRequestId(), errorMsg);
            if (updated > 0) {
                request.setStatus(PaymentStatus.FAILED.getCode());
                request.setErrorMsg(errorMsg);
                logger.debug("Marked request as FAILED: {}", request.getRequestId());
            } else {
                logger.warn("Failed to mark request as FAILED (0 rows updated): {}", request.getRequestId());
            }
        } catch (Exception e) {
            logger.error("Failed to mark request as FAILED: {}", request.getRequestId(), e);
        }
    }

    /**
     * 标记单个请求为TIMEOUT状态
     */
    private void markRequestTimeout(PaymentRequest request, String errorMsg) {
        try {
            int updated = paymentRequestRepository.updateToTimeout(request.getRequestId(), errorMsg);
            if (updated > 0) {
                request.setStatus(PaymentStatus.TIMEOUT.getCode());
                request.setErrorMsg(errorMsg);
                logger.debug("Marked request as TIMEOUT: {}", request.getRequestId());
            } else {
                logger.warn("Failed to mark request as TIMEOUT (0 rows updated): {}", request.getRequestId());
            }
        } catch (Exception e) {
            logger.error("Failed to mark request as TIMEOUT: {}", request.getRequestId(), e);
        }
    }

    /**
     * 处理批次结果
     * 根据第三方接口返回结果更新状态
     */
    private void handleBatchResult(List<PaymentRequest> batch, PaymentResult result) {
        PaymentStatus status = result.getStatus();
        String batchId = result.getBatchId();

        if (status == null) {
            logger.error("Batch result status is null, batchId: {}", batchId);
            return;
        }

        switch (status) {
            case PROCESSING:
                // 第三方已接受，更新为PROCESSING状态，等待异步通知
                markBatchProcessing(batch, batchId);
                logger.info("Batch payment accepted, updated to PROCESSING, waiting for async notification, batchId: {}",
                        batchId);
                break;

            case SUCCESS:
                // 同步返回成功，直接标记为SUCCESS
                markBatchSuccess(batch);
                metrics.recordSuccess();
                logger.info("Batch payment success, batchId: {}", batchId);
                break;

            case FAILED:
                // 业务失败，标记为FAILED
                markBatchFailed(batch, result.getMessage());
                metrics.recordFailure();
                logger.warn("Batch payment failed (business), batchId: {}, message: {}",
                        batchId, result.getMessage());
                break;

            case TIMEOUT:
                // 接口超时，标记为TIMEOUT（可重试）
                markBatchTimeout(batch, result.getMessage());
                metrics.recordTimeout();
                metrics.recordThirdPartyTimeout();
                logger.warn("Batch payment timeout, batchId: {}, message: {}",
                        batchId, result.getMessage());
                break;

            default:
                // 其他异常状态
                logger.warn("Batch payment unknown status: {}, batchId: {}", status, batchId);
                break;
        }
    }

    /**
     * 标记批次为PROCESSING状态（第三方已接受）
     * 使用CAS操作：UPDATE ... WHERE status = 'PENDING'
     */
    private void markBatchProcessing(List<PaymentRequest> batch, String batchId) {
        int successCount = 0;
        int failCount = 0;

        for (PaymentRequest request : batch) {
            String requestId = request.getRequestId();

            try {
                // 首先查询当前数据库状态
                PaymentRequest beforeUpdate = paymentRequestRepository.findById(requestId);
                if (beforeUpdate == null) {
                    logger.error("Request not found in DB before updateToProcessing: {}", requestId);
                    failCount++;
                    continue;
                }
                logger.debug("Before updateToProcessing: requestId={}, dbStatus={}, memoryStatus={}",
                        requestId, beforeUpdate.getStatus(), request.getStatus());

                // CAS操作：只有status=PENDING时才更新
                int updated = paymentRequestRepository.updateToProcessing(requestId, batchId);

                if (updated > 0) {
                    // 更新成功
                    request.setStatus(PaymentStatus.PROCESSING.getCode());
                    successCount++;
                    logger.debug("Marked request as PROCESSING: {}", requestId);
                } else {
                    // 更新失败，查询最新状态并更新内存
                    PaymentRequest latest = paymentRequestRepository.findById(requestId);
                    if (latest != null) {
                        request.setStatus(latest.getStatus());
                        request.setBatchId(latest.getBatchId());
                        logger.warn("Failed to mark request as PROCESSING (not PENDING in DB): {}, currentStatus={}, dbBatchId={}",
                                requestId, latest.getStatus(), latest.getBatchId());
                    } else {
                        logger.error("Request not found when marking PROCESSING: {}", requestId);
                    }
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to mark request as PROCESSING: {}", requestId, e);
                failCount++;
            }
        }

        logger.info("Batch PROCESSING marking completed: success={}, fail={}, batchId={}",
                successCount, failCount, batchId);
    }

    /**
     * 处理批次异常
     */
    private void handleBatchException(List<PaymentRequest> batch, String batchId, Exception e) {
        metrics.recordFailure();

        // 异常时保持PENDING状态，不更新为PROCESSING
        // 等待定时任务重试或人工处理
        logger.error("Batch processing exception, keeping PENDING state, batchId: {}", batchId, e);
    }

    /**
     * 标记批次成功
     */
    private void markBatchSuccess(List<PaymentRequest> batch) {
        int successCount = 0;
        int failCount = 0;

        for (PaymentRequest request : batch) {
            String requestId = request.getRequestId();
            try {
                // 先检查请求是否存在
                PaymentRequest dbRequest = paymentRequestRepository.findById(requestId);
                if (dbRequest == null) {
                    logger.error("Request not found in DB before markBatchSuccess: {}", requestId);
                    failCount++;
                    continue;
                }

                int updated = paymentRequestRepository.updateToSuccess(requestId);
                if (updated > 0) {
                    request.setStatus(PaymentStatus.SUCCESS.getCode());
                    successCount++;
                    logger.debug("Marked request as SUCCESS: {}", requestId);
                } else {
                    logger.warn("Failed to mark request as SUCCESS (0 rows updated): {}", requestId);
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to mark success for request: {}", requestId, e);
                failCount++;
            }
        }

        logger.info("Batch SUCCESS marking completed: success={}, fail={}, batchSize={}",
                successCount, failCount, batch.size());
    }

    /**
     * 标记批次失败
     */
    private void markBatchFailed(List<PaymentRequest> batch, String errorMsg) {
        int successCount = 0;
        int failCount = 0;

        for (PaymentRequest request : batch) {
            String requestId = request.getRequestId();
            try {
                // 先检查请求是否存在
                PaymentRequest dbRequest = paymentRequestRepository.findById(requestId);
                if (dbRequest == null) {
                    logger.error("Request not found in DB before markBatchFailed: {}", requestId);
                    failCount++;
                    continue;
                }

                int updated = paymentRequestRepository.updateToFailed(requestId, errorMsg);
                if (updated > 0) {
                    request.setStatus(PaymentStatus.FAILED.getCode());
                    request.setErrorMsg(errorMsg);
                    successCount++;
                    logger.debug("Marked request as FAILED: {}, errorMsg={}", requestId, errorMsg);
                } else {
                    logger.warn("Failed to mark request as FAILED (0 rows updated): {}", requestId);
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to mark failed for request: {}", requestId, e);
                failCount++;
            }
        }

        logger.info("Batch FAILED marking completed: success={}, fail={}, batchSize={}",
                successCount, failCount, batch.size());
    }

    /**
     * 标记批次超时
     */
    private void markBatchTimeout(List<PaymentRequest> batch, String errorMsg) {
        int successCount = 0;
        int failCount = 0;

        for (PaymentRequest request : batch) {
            String requestId = request.getRequestId();
            try {
                // 先检查请求是否存在
                PaymentRequest dbRequest = paymentRequestRepository.findById(requestId);
                if (dbRequest == null) {
                    logger.error("Request not found in DB before markBatchTimeout: {}", requestId);
                    failCount++;
                    continue;
                }

                int updated = paymentRequestRepository.updateToTimeout(requestId, errorMsg);
                if (updated > 0) {
                    request.setStatus(PaymentStatus.TIMEOUT.getCode());
                    request.setErrorMsg(errorMsg);
                    successCount++;
                    logger.debug("Marked request as TIMEOUT: {}, errorMsg={}", requestId, errorMsg);
                } else {
                    logger.warn("Failed to mark request as TIMEOUT (0 rows updated): {}", requestId);
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to mark timeout for request: {}", requestId, e);
                failCount++;
            }
        }

        logger.info("Batch TIMEOUT marking completed: success={}, fail={}, batchSize={}",
                successCount, failCount, batch.size());
    }

    /**
     * 处理异步通知结果（第三方回调）
     * 状态从 PROCESSING -> SUCCESS/FAILED
     */
    public void handleAsyncNotify(String requestId, boolean success, String message) {
        try {
            PaymentRequest request = paymentRequestRepository.findById(requestId);
            if (request == null) {
                logger.error("Request not found: {}", requestId);
                return;
            }

            // 只能处理PROCESSING状态的请求
            if (!PaymentStatus.PROCESSING.getCode().equals(request.getStatus())) {
                logger.warn("Request status is not PROCESSING: {}, status: {}",
                        requestId, request.getStatus());
                return;
            }

            if (success) {
                paymentRequestRepository.updateToSuccess(requestId);
                logger.info("Async notify success for request: {}", requestId);
            } else {
                paymentRequestRepository.updateToFailed(requestId, message);
                logger.warn("Async notify failed for request: {}, message: {}",
                        requestId, message);
            }
        } catch (Exception e) {
            logger.error("Failed to handle async notify: {}", requestId, e);
        }
    }

    /**
     * 计算每秒请求数（QPS）
     */
    private void calculateQps() {
        long count = secondRequestCount.getAndSet(0);
        metrics.recordSecondRequests(count);
        logger.debug("Current QPS: {}", count);
    }

    /**
     * 获取所有线程状态
     */
    public List<ThreadStatus> getThreadStatusList() {
        List<ThreadStatus> result = new ArrayList<>();
        
        // 获取线程池的核心线程数
        int corePoolSize = batchProcessor.getCorePoolSize();
        int activeCount = batchProcessor.getActiveCount();
        
        // 首先添加threadStatusMap中记录的活跃线程
        List<ThreadStatus> activeThreads = threadStatusMap.values().stream()
                .filter(ThreadStatus::isActive)
                .collect(Collectors.toList());
        result.addAll(activeThreads);
        
        // 计算空闲线程数量
        int idleCount = corePoolSize - activeThreads.size();
        
        // 为空闲线程创建状态对象
        for (int i = 0; i < idleCount; i++) {
            String threadName = "payment-batch-processor-" + i;
            // 如果该线程已经在活跃列表中，跳过
            if (activeThreads.stream().noneMatch(t -> t.getThreadName().equals(threadName))) {
                result.add(new ThreadStatus(threadName, 0, false, 0, null));
            }
        }
        
        return result;
    }

    /**
     * 获取当前队列状态
     */
    public QueueStatus getQueueStatus() {
        return new QueueStatus(
                getTotalQueueSize(),
                vipQueue.size(),
                normalQueue.size(),
                metrics.getLastSecondRequests(),
                metrics.getTotalRequests(),
                metrics.getSuccessRequests(),
                metrics.getFailedRequests(),
                metrics.getTimeoutRequests(),
                metrics.getTotalBatches(),
                batchProcessor.getActiveCount(),
                batchProcessor.getQueue().size(),
                batchProcessor.getQueue().size(),
                1000, // 线程池队列容量（与构造函数中的 LinkedBlockingQueue<>(1000) 一致）
                metrics.getAvgProcessTime(),
                metrics.getMaxProcessTime(),
                metrics.getMinProcessTime(),
                metrics.getAvgThirdPartyTime(),
                metrics.getThirdPartyTimeoutRate(),
                metrics.getThirdPartyTps(),
                metrics.getAvgQueueWaitTime(),
                metrics.getProcessThroughput()
        );
    }

    /**
     * 队列状态信息
     */
    public static class QueueStatus {
        // 基础指标
        private final int queueSize;
        private final int vipQueueSize;
        private final int normalQueueSize;
        private final long currentQps;
        private final long totalRequests;
        private final long successRequests;
        private final long failedRequests;
        private final long timeoutRequests;
        private final long totalBatches;

        // 线程池指标
        private final int activeThreads;
        private final int waitingTasks;
        private final int threadPoolQueueSize;    // 线程池队列当前大小
        private final int threadPoolQueueCapacity; // 线程池队列容量

        // 处理耗时指标
        private final double avgProcessTime;
        private final long maxProcessTime;
        private final long minProcessTime;

        // 第三方接口指标
        private final double avgThirdPartyTime;
        private final double thirdPartyTimeoutRate;
        private final double thirdPartyTps;

        // 队列等待指标
        private final double avgQueueWaitTime;

        // 吞吐量指标
        private final double processThroughput;

        public QueueStatus(int queueSize, int vipQueueSize, int normalQueueSize,
                          long currentQps, long totalRequests,
                          long successRequests, long failedRequests, long timeoutRequests,
                          long totalBatches, int activeThreads, int waitingTasks,
                          int threadPoolQueueSize, int threadPoolQueueCapacity,
                          double avgProcessTime, long maxProcessTime, long minProcessTime,
                          double avgThirdPartyTime, double thirdPartyTimeoutRate,
                          double thirdPartyTps, double avgQueueWaitTime, double processThroughput) {
            this.queueSize = queueSize;
            this.vipQueueSize = vipQueueSize;
            this.normalQueueSize = normalQueueSize;
            this.currentQps = currentQps;
            this.totalRequests = totalRequests;
            this.successRequests = successRequests;
            this.failedRequests = failedRequests;
            this.timeoutRequests = timeoutRequests;
            this.totalBatches = totalBatches;
            this.activeThreads = activeThreads;
            this.waitingTasks = waitingTasks;
            this.threadPoolQueueSize = threadPoolQueueSize;
            this.threadPoolQueueCapacity = threadPoolQueueCapacity;
            this.avgProcessTime = avgProcessTime;
            this.maxProcessTime = maxProcessTime;
            this.minProcessTime = minProcessTime;
            this.avgThirdPartyTime = avgThirdPartyTime;
            this.thirdPartyTimeoutRate = thirdPartyTimeoutRate;
            this.thirdPartyTps = thirdPartyTps;
            this.avgQueueWaitTime = avgQueueWaitTime;
            this.processThroughput = processThroughput;
        }

        // Getters
        public int getQueueSize() { return queueSize; }
        public int getVipQueueSize() { return vipQueueSize; }
        public int getNormalQueueSize() { return normalQueueSize; }
        public long getCurrentQps() { return currentQps; }
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessRequests() { return successRequests; }
        public long getFailedRequests() { return failedRequests; }
        public long getTimeoutRequests() { return timeoutRequests; }
        public long getTotalBatches() { return totalBatches; }
        public int getActiveThreads() { return activeThreads; }
        public int getWaitingTasks() { return waitingTasks; }
        public int getThreadPoolQueueSize() { return threadPoolQueueSize; }
        public int getThreadPoolQueueCapacity() { return threadPoolQueueCapacity; }
        public double getAvgProcessTime() { return avgProcessTime; }
        public long getMaxProcessTime() { return maxProcessTime; }
        public long getMinProcessTime() { return minProcessTime; }
        public double getAvgThirdPartyTime() { return avgThirdPartyTime; }
        public double getThirdPartyTimeoutRate() { return thirdPartyTimeoutRate; }
        public double getThirdPartyTps() { return thirdPartyTps; }
        public double getAvgQueueWaitTime() { return avgQueueWaitTime; }
        public double getProcessThroughput() { return processThroughput; }
    }

    /**
     * 线程状态信息
     */
    public static class ThreadStatus {
        private final String threadName;
        private final int processingCount;  // 当前处理的元素数量
        private final boolean isActive;     // 是否正在消费
        private final long startTime;       // 开始处理时间
        private final String batchId;       // 当前处理的批次ID

        public ThreadStatus(String threadName, int processingCount, boolean isActive, long startTime, String batchId) {
            this.threadName = threadName;
            this.processingCount = processingCount;
            this.isActive = isActive;
            this.startTime = startTime;
            this.batchId = batchId;
        }

        public String getThreadName() { return threadName; }
        public int getProcessingCount() { return processingCount; }
        public boolean isActive() { return isActive; }
        public long getStartTime() { return startTime; }
        public String getBatchId() { return batchId; }
    }
}
