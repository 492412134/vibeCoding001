package com.example.payment.payment.service;

import com.example.payment.payment.enums.PolicyType;
import com.example.payment.payment.id.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 雪花算法ID生成服务
 */
@Service
public class SnowflakeIdService {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdService.class);

    @Autowired
    private SnowflakeIdGenerator idGenerator;

    @Value("${server.port:10991}") // 从配置文件读取服务器端口，默认值为10991
    private String serverPort;

    // 并发测试的线程池
    private final ExecutorService concurrentTestExecutor = Executors.newFixedThreadPool(20);

    /**
     * 生成单个ID
     */
    public IdGenerationResult generateSingleId(String policyTypeName, long policyCode) {
        PolicyType policyType = PolicyType.fromName(policyTypeName);
        long startTime = System.currentTimeMillis();

        long id = idGenerator.generateId(policyType, policyCode);

        long generateTime = System.currentTimeMillis() - startTime;

        IdGenerationResult result = new IdGenerationResult();
        result.setId(id);
        result.setIdString(String.valueOf(id));
        result.setBinaryString(idGenerator.getIdBinaryString(id));
        result.setStructureString(idGenerator.getIdStructureString(id));
        result.setPolicyType(policyType);
        result.setPolicyCode(policyCode);
        result.setTableIndex(policyCode % 16);
        result.setGenerateTime(generateTime);
        result.setServerPort(serverPort);
        result.setGenerateTime(LocalDateTime.now());

        // 解析ID信息
        SnowflakeIdGenerator.IdInfo idInfo = idGenerator.parseId(id);
        result.setIdInfo(idInfo);

        return result;
    }

    /**
     * 批量生成ID
     */
    public BatchIdGenerationResult generateBatchIds(String policyTypeName, long policyCode, int count) {
        PolicyType policyType = PolicyType.fromName(policyTypeName);
        long startTime = System.currentTimeMillis();

        List<IdGenerationResult> results = new ArrayList<>();
        Set<Long> uniqueIds = new HashSet<>();

        for (int i = 0; i < count; i++) {
            IdGenerationResult result = generateSingleId(policyTypeName, policyCode);
            results.add(result);
            uniqueIds.add(result.getId());
        }

        long totalTime = System.currentTimeMillis() - startTime;

        BatchIdGenerationResult batchResult = new BatchIdGenerationResult();
        batchResult.setResults(results);
        batchResult.setTotalCount(count);
        batchResult.setUniqueCount(uniqueIds.size());
        batchResult.setTotalTime(totalTime);
        batchResult.setServerPort(serverPort);
        batchResult.setAllUnique(count == uniqueIds.size());

        return batchResult;
    }

    /**
     * 并发测试 - 模拟多线程生成ID
     */
    public ConcurrentTestResult concurrentTest(String policyTypeName, long policyCode, int totalCount, int concurrentThreads) {
        long testStartTime = System.currentTimeMillis();

        PolicyType policyType = PolicyType.fromName(policyTypeName);
        List<IdGenerationResult> allResults = Collections.synchronizedList(new ArrayList<>());
        Set<Long> uniqueIds = ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 计算每个线程需要生成的ID数量
        int countPerThread = totalCount / concurrentThreads;
        int remainder = totalCount % concurrentThreads;

        CountDownLatch latch = new CountDownLatch(concurrentThreads);

        for (int i = 0; i < concurrentThreads; i++) {
            final int threadIndex = i;
            final int threadCount = countPerThread + (i < remainder ? 1 : 0);

            concurrentTestExecutor.submit(() -> {
                try {
                    for (int j = 0; j < threadCount; j++) {
                        try {
                            IdGenerationResult result = generateSingleId(policyTypeName, policyCode);
                            allResults.add(result);
                            uniqueIds.add(result.getId());
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            logger.error("Error generating ID in thread {}", threadIndex, e);
                            failCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Concurrent test interrupted", e);
            Thread.currentThread().interrupt();
        }

        long testTotalTime = System.currentTimeMillis() - testStartTime;

        ConcurrentTestResult result = new ConcurrentTestResult();
        result.setResults(allResults);
        result.setTotalCount(totalCount);
        result.setSuccessCount(successCount.get());
        result.setFailCount(failCount.get());
        result.setUniqueCount(uniqueIds.size());
        result.setTotalTime(testTotalTime);
        result.setServerPort(serverPort);
        result.setConcurrentThreads(concurrentThreads);
        result.setAllUnique(totalCount == uniqueIds.size());
        result.setThroughput((double) successCount.get() / testTotalTime * 1000); // IDs per second
        return result;
    }

    /**
     * 获取当前节点信息
     */
    public NodeInfo getNodeInfo() {
        NodeInfo info = new NodeInfo();
        info.setServerPort(serverPort);
        info.setTimestamp(System.currentTimeMillis());
        info.setDateTime(LocalDateTime.now());
        return info;
    }

    /**
     * ID生成结果
     */
    public static class IdGenerationResult {
        private long id;
        private String idString;
        private String binaryString;
        private String structureString;
        private PolicyType policyType;
        private long policyCode;
        private long tableIndex;
        private long generateTime;
        private String serverPort;
        private LocalDateTime generateTimeDate;
        private SnowflakeIdGenerator.IdInfo idInfo;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getIdString() { return idString; }
        public void setIdString(String idString) { this.idString = idString; }

        public String getBinaryString() { return binaryString; }
        public void setBinaryString(String binaryString) { this.binaryString = binaryString; }

        public String getStructureString() { return structureString; }
        public void setStructureString(String structureString) { this.structureString = structureString; }

        public PolicyType getPolicyType() { return policyType; }
        public void setPolicyType(PolicyType policyType) { this.policyType = policyType; }

        public long getPolicyCode() { return policyCode; }
        public void setPolicyCode(long policyCode) { this.policyCode = policyCode; }

        public long getTableIndex() { return tableIndex; }
        public void setTableIndex(long tableIndex) { this.tableIndex = tableIndex; }

        public long getGenerateTime() { return generateTime; }
        public void setGenerateTime(long generateTime) { this.generateTime = generateTime; }

        public String getServerPort() { return serverPort; }
        public void setServerPort(String serverPort) { this.serverPort = serverPort; }

        public LocalDateTime getGenerateTimeDate() { return generateTimeDate; }
        public void setGenerateTime(LocalDateTime generateTimeDate) { this.generateTimeDate = generateTimeDate; }

        public SnowflakeIdGenerator.IdInfo getIdInfo() { return idInfo; }
        public void setIdInfo(SnowflakeIdGenerator.IdInfo idInfo) { this.idInfo = idInfo; }
    }

    /**
     * 批量ID生成结果
     */
    public static class BatchIdGenerationResult {
        private List<IdGenerationResult> results;
        private int totalCount;
        private int uniqueCount;
        private long totalTime;
        private String serverPort;
        private boolean allUnique;

        public List<IdGenerationResult> getResults() { return results; }
        public void setResults(List<IdGenerationResult> results) { this.results = results; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getUniqueCount() { return uniqueCount; }
        public void setUniqueCount(int uniqueCount) { this.uniqueCount = uniqueCount; }

        public long getTotalTime() { return totalTime; }
        public void setTotalTime(long totalTime) { this.totalTime = totalTime; }

        public String getServerPort() { return serverPort; }
        public void setServerPort(String serverPort) { this.serverPort = serverPort; }

        public boolean isAllUnique() { return allUnique; }
        public void setAllUnique(boolean allUnique) { this.allUnique = allUnique; }
    }

    /**
     * 并发测试结果
     */
    public static class ConcurrentTestResult {
        private List<IdGenerationResult> results;
        private int totalCount;
        private int successCount;
        private int failCount;
        private int uniqueCount;
        private long totalTime;
        private String serverPort;
        private int concurrentThreads;
        private boolean allUnique;
        private double throughput;

        public List<IdGenerationResult> getResults() { return results; }
        public void setResults(List<IdGenerationResult> results) { this.results = results; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }

        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }

        public int getUniqueCount() { return uniqueCount; }
        public void setUniqueCount(int uniqueCount) { this.uniqueCount = uniqueCount; }

        public long getTotalTime() { return totalTime; }
        public void setTotalTime(long totalTime) { this.totalTime = totalTime; }

        public String getServerPort() { return serverPort; }
        public void setServerPort(String serverPort) { this.serverPort = serverPort; }

        public int getConcurrentThreads() { return concurrentThreads; }
        public void setConcurrentThreads(int concurrentThreads) { this.concurrentThreads = concurrentThreads; }

        public boolean isAllUnique() { return allUnique; }
        public void setAllUnique(boolean allUnique) { this.allUnique = allUnique; }

        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
    }

    /**
     * 节点信息
     */
    public static class NodeInfo {
        private String serverPort;
        private long timestamp;
        private LocalDateTime dateTime;

        public String getServerPort() { return serverPort; }
        public void setServerPort(String serverPort) { this.serverPort = serverPort; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public LocalDateTime getDateTime() { return dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }
    }
}
