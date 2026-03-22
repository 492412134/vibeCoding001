package com.example.vibecoding001.payment.id;

import com.example.vibecoding001.payment.enums.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * 雪花算法ID生成器（基因法变种）
 *
 * ID结构（64位）：
 * | 时间戳(41位) | 政策类型(4位) | 政策分表(8位) | 序列号(11位) |
 * | 0-41       | 41-45        | 45-53       | 53-64      |
 *
 * 时间戳：毫秒级，可使用69年
 * 政策类型：4位，支持16种类型
 * 政策分表：8位，支持256张表（实际使用16张）
 * 序列号：11位，每毫秒最多2048个ID
 */
@Component
public class SnowflakeIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    // 起始时间戳（2024-01-01 00:00:00）
    private static final long START_TIMESTAMP = 1704067200000L;

    // 各部分的位数

    private static final long TIMESTAMP_BITS = 41L;
    private static final long POLICY_TYPE_BITS = 4L;
    private static final long TABLE_INDEX_BITS = 8L;
    private static final long SEQUENCE_BITS = 11L;

    // 各部分的最大值
    private static final long MAX_POLICY_TYPE = ~(-1L << POLICY_TYPE_BITS);  // 15
    private static final long MAX_TABLE_INDEX = ~(-1L << TABLE_INDEX_BITS);  // 255
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);        // 2047

    // 各部分的位移
    private static final long POLICY_TYPE_SHIFT = SEQUENCE_BITS + TABLE_INDEX_BITS;
    private static final long TABLE_INDEX_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = POLICY_TYPE_BITS + TABLE_INDEX_BITS + SEQUENCE_BITS;

    // Redis序列号Key前缀
    private static final String REDIS_SEQUENCE_KEY_PREFIX = "snowflake:seq:";

    // 序列号过期时间（秒）
    private static final long SEQUENCE_EXPIRE_SECONDS = 60;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 上一次生成ID的时间戳
    private long lastTimestamp = -1L;

    /**
     * 生成唯一ID
     *
     * @param policyType 政策类型
     * @param policyCode 政策编号
     * @return 生成的唯一ID
     */
    public synchronized long generateId(PolicyType policyType, long policyCode) {
        long currentTimestamp = getCurrentTimestamp();

        // 时钟回拨检查
        if (currentTimestamp < lastTimestamp) {
            logger.error("Clock moved backwards. Refusing to generate id for {} milliseconds", lastTimestamp - currentTimestamp);
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        // 计算政策分表索引（policyCode % 16）
        long tableIndex = policyCode % 16;

        // 从Redis获取序列号
        long sequence = getSequenceFromRedis(policyType, policyCode);

        // 生成ID
        long id = ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | ((policyType.getCode() & MAX_POLICY_TYPE) << POLICY_TYPE_SHIFT)
                | ((tableIndex & MAX_TABLE_INDEX) << TABLE_INDEX_SHIFT)
                | (sequence & MAX_SEQUENCE);

        lastTimestamp = currentTimestamp;

        logger.debug("Generated snowflake id: {}, policyType: {}, policyCode: {}, tableIndex: {}, sequence: {}",
                id, policyType.getName(), policyCode, tableIndex, sequence);

        return id;
    }

    /**
     * 批量生成ID
     *
     * @param policyType 政策类型
     * @param policyCode 政策编号
     * @param count 生成数量
     * @return ID数组
     */
    public long[] generateBatchIds(PolicyType policyType, long policyCode, int count) {
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            ids[i] = generateId(policyType, policyCode);
        }
        return ids;
    }

    /**
     * 从Redis获取序列号
     * 使用Redis的INCR原子操作，确保并发安全
     */
    private long getSequenceFromRedis(PolicyType policyType, long policyCode) {
        String key = REDIS_SEQUENCE_KEY_PREFIX + policyType.getName() + ":" + policyCode;

        try {
            // 使用INCR获取递增的序列号
            Long sequence = redisTemplate.opsForValue().increment(key);

            if (sequence == null) {
                sequence = 0L;
            }

            // 设置过期时间（首次设置）
            // redisTemplate.expire(key, SEQUENCE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            

            // 序列号范围控制在0-2047
            return sequence % (MAX_SEQUENCE + 1);

        } catch (Exception e) {
            logger.error("Failed to get sequence from Redis, key: {}", key, e);
            // Redis故障时，使用本地序列号作为降级方案
            return System.currentTimeMillis() % (MAX_SEQUENCE + 1);
        }
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 解析ID，获取各部分信息
     */
    public IdInfo parseId(long id) {
        IdInfo info = new IdInfo();

        // 解析序列号（低11位）
        info.setSequence(id & MAX_SEQUENCE);

        // 解析政策分表（接下来8位）
        info.setTableIndex((id >> TABLE_INDEX_SHIFT) & MAX_TABLE_INDEX);

        // 解析政策类型（接下来4位）
        int policyTypeCode = (int) ((id >> POLICY_TYPE_SHIFT) & MAX_POLICY_TYPE);
        info.setPolicyType(PolicyType.fromCode(policyTypeCode));

        // 解析时间戳（高41位）
        long timestamp = (id >> TIMESTAMP_SHIFT) + START_TIMESTAMP;
        info.setTimestamp(timestamp);
        info.setDateTime(LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC));

        return info;
    }

    /**
     * ID信息类
     */
    public static class IdInfo {
        private long timestamp;
        private LocalDateTime dateTime;
        private PolicyType policyType;
        private long tableIndex;
        private long sequence;

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public void setDateTime(LocalDateTime dateTime) {
            this.dateTime = dateTime;
        }

        public PolicyType getPolicyType() {
            return policyType;
        }

        public void setPolicyType(PolicyType policyType) {
            this.policyType = policyType;
        }

        public long getTableIndex() {
            return tableIndex;
        }

        public void setTableIndex(long tableIndex) {
            this.tableIndex = tableIndex;
        }

        public long getSequence() {
            return sequence;
        }

        public void setSequence(long sequence) {
            this.sequence = sequence;
        }

        @Override
        public String toString() {
            return String.format("IdInfo{timestamp=%d, dateTime=%s, policyType=%s, tableIndex=%d, sequence=%d}",
                    timestamp, dateTime, policyType, tableIndex, sequence);
        }
    }

    /**
     * 获取ID的二进制字符串表示
     */
    public String getIdBinaryString(long id) {
        return String.format("%64s", Long.toBinaryString(id)).replace(' ', '0');
    }

    /**
     * 获取ID结构的格式化字符串
     */
    public String getIdStructureString(long id) {
        String binary = getIdBinaryString(id);
        // 41位时间戳 | 4位政策类型 | 8位分表 | 11位序列号
        return binary.substring(0, 41) + " | " +
               binary.substring(41, 45) + " | " +
               binary.substring(45, 53) + " | " +
               binary.substring(53, 64);
    }
}
