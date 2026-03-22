package com.example.vibecoding001.payment.controller;

import com.example.vibecoding001.payment.service.SnowflakeIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 雪花算法ID生成控制器
 */
@RestController
@RequestMapping("/api/snowflake")
public class SnowflakeIdController {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdController.class);

    @Autowired
    private SnowflakeIdService snowflakeIdService;

    /**
     * 生成单个ID
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateId(@RequestBody Map<String, Object> request) {
        logger.info("Received generate ID request: {}", request);

        try {
            String policyType = (String) request.getOrDefault("policyType", "common");
            long policyCode = Long.parseLong(request.getOrDefault("policyCode", "33010011").toString());

            SnowflakeIdService.IdGenerationResult result = snowflakeIdService.generateSingleId(policyType, policyCode);

            Map<String, Object> data = new HashMap<>();
            data.put("id", result.getId());
            data.put("idString", result.getIdString());
            data.put("binaryString", result.getBinaryString());
            data.put("structureString", result.getStructureString());
            data.put("policyType", result.getPolicyType().getName());
            data.put("policyTypeCode", result.getPolicyType().getCode());
            data.put("policyTypeBinary", result.getPolicyType().getBinaryString());
            data.put("policyCode", result.getPolicyCode());
            data.put("tableIndex", result.getTableIndex());
            data.put("generateTime", result.getGenerateTime());
            data.put("serverPort", result.getServerPort());
            data.put("generateTimeDate", result.getGenerateTimeDate().toString());

            // ID详细信息
            Map<String, Object> idInfo = new HashMap<>();
            idInfo.put("timestamp", result.getIdInfo().getTimestamp());
            idInfo.put("dateTime", result.getIdInfo().getDateTime().toString());
            idInfo.put("policyType", result.getIdInfo().getPolicyType().getName());
            idInfo.put("tableIndex", result.getIdInfo().getTableIndex());
            idInfo.put("sequence", result.getIdInfo().getSequence());
            data.put("idInfo", idInfo);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "ID generated successfully",
                    "data", data
            ));
        } catch (Exception e) {
            logger.error("Failed to generate ID", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 500,
                    "message", "Failed to generate ID: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量生成ID
     */
    @PostMapping("/generate/batch")
    public ResponseEntity<Map<String, Object>> generateBatchIds(@RequestBody Map<String, Object> request) {
        logger.info("Received batch generate ID request: {}", request);

        try {
            String policyType = (String) request.getOrDefault("policyType", "common");
            long policyCode = Long.parseLong(request.getOrDefault("policyCode", "33010011").toString());
            int count = Integer.parseInt(request.getOrDefault("count", "10").toString());

            // 限制批量生成数量
            if (count > 1000) {
                count = 1000;
            }

            SnowflakeIdService.BatchIdGenerationResult batchResult = snowflakeIdService.generateBatchIds(policyType, policyCode, count);

            Map<String, Object> data = new HashMap<>();
            data.put("totalCount", batchResult.getTotalCount());
            data.put("uniqueCount", batchResult.getUniqueCount());
            data.put("totalTime", batchResult.getTotalTime());
            data.put("serverPort", batchResult.getServerPort());
            data.put("allUnique", batchResult.isAllUnique());

            // 简化结果，只返回ID列表
            java.util.List<Map<String, Object>> idList = new java.util.ArrayList<>();
            for (SnowflakeIdService.IdGenerationResult result : batchResult.getResults()) {
                Map<String, Object> idMap = new HashMap<>();
                idMap.put("id", result.getId());
                idMap.put("tableIndex", result.getTableIndex());
                idMap.put("structure", result.getStructureString());
                idMap.put("serverPort", result.getServerPort());
                idList.add(idMap);
            }
            data.put("ids", idList);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "Batch IDs generated successfully",
                    "data", data
            ));
        } catch (Exception e) {
            logger.error("Failed to generate batch IDs", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 500,
                    "message", "Failed to generate batch IDs: " + e.getMessage()
            ));
        }
    }

    /**
     * 并发测试
     */
    @PostMapping("/concurrent-test")
    public ResponseEntity<Map<String, Object>> concurrentTest(@RequestBody Map<String, Object> request) {
        logger.info("Received concurrent test request: {}", request);

        try {
            String policyType = (String) request.getOrDefault("policyType", "common");
            long policyCode = Long.parseLong(request.getOrDefault("policyCode", "33010011").toString());
            int totalCount = Integer.parseInt(request.getOrDefault("totalCount", "100").toString());
            int concurrentThreads = Integer.parseInt(request.getOrDefault("concurrentThreads", "10").toString());

            // 限制参数
            if (totalCount > 1000) {
                totalCount = 1000;
            }
            if (concurrentThreads > 50) {
                concurrentThreads = 50;
            }

            SnowflakeIdService.ConcurrentTestResult testResult = snowflakeIdService.concurrentTest(policyType, policyCode, totalCount, concurrentThreads);

            Map<String, Object> data = new HashMap<>();
            data.put("totalCount", testResult.getTotalCount());
            data.put("successCount", testResult.getSuccessCount());
            data.put("failCount", testResult.getFailCount());
            data.put("uniqueCount", testResult.getUniqueCount());
            data.put("totalTime", testResult.getTotalTime());
            data.put("serverPort", testResult.getServerPort());
            data.put("concurrentThreads", testResult.getConcurrentThreads());
            data.put("allUnique", testResult.isAllUnique());
            data.put("throughput", String.format("%.2f", testResult.getThroughput()));

            // 简化结果，只返回ID列表
            java.util.List<Map<String, Object>> idList = new java.util.ArrayList<>();
            for (SnowflakeIdService.IdGenerationResult result : testResult.getResults()) {
                Map<String, Object> idMap = new HashMap<>();
                idMap.put("id", result.getId());
                idMap.put("serverPort", result.getServerPort());
                idMap.put("tableIndex", result.getTableIndex());
                idMap.put("structure", result.getStructureString());
                idList.add(idMap);
            }
            data.put("ids", idList);

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "Concurrent test completed",
                    "data", data
            ));
        } catch (Exception e) {
            logger.error("Concurrent test failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 500,
                    "message", "Concurrent test failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前节点信息
     */
    @GetMapping("/node-info")
    public ResponseEntity<Map<String, Object>> getNodeInfo() {
        try {
            SnowflakeIdService.NodeInfo nodeInfo = snowflakeIdService.getNodeInfo();

            Map<String, Object> data = new HashMap<>();
            data.put("serverPort", nodeInfo.getServerPort());
            data.put("timestamp", nodeInfo.getTimestamp());
            data.put("dateTime", nodeInfo.getDateTime().toString());

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "success",
                    "data", data
            ));
        } catch (Exception e) {
            logger.error("Failed to get node info", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 500,
                    "message", "Failed to get node info: " + e.getMessage()
            ));
        }
    }

    /**
     * 解析ID
     */
    @GetMapping("/parse/{id}")
    public ResponseEntity<Map<String, Object>> parseId(@PathVariable long id) {
        try {
            // 这里可以通过service调用generator的parseId方法
            // 简化实现，直接返回ID的二进制表示
            String binaryString = String.format("%64s", Long.toBinaryString(id)).replace(' ', '0');

            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("binaryString", binaryString);
            data.put("structure", binaryString.substring(0, 41) + " | " +
                    binaryString.substring(41, 45) + " | " +
                    binaryString.substring(45, 53) + " | " +
                    binaryString.substring(53, 64));

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "success",
                    "data", data
            ));
        } catch (Exception e) {
            logger.error("Failed to parse ID", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 500,
                    "message", "Failed to parse ID: " + e.getMessage()
            ));
        }
    }
}
