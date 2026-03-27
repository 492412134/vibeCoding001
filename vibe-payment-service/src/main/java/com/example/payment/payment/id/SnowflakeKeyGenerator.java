package com.example.payment.payment.id;

import com.example.payment.payment.enums.PolicyType;
import org.apache.shardingsphere.infra.algorithm.core.context.AlgorithmSQLContext;
import org.apache.shardingsphere.infra.algorithm.keygen.core.KeyGenerateAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Properties;

/**
 * 自定义雪花算法KeyGenerator
 * 用于ShardingSphere 5.5.x 的分布式主键生成
 */
@Component
public class SnowflakeKeyGenerator implements KeyGenerateAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeKeyGenerator.class);

    private static SnowflakeIdGenerator idGenerator;
    private static PolicyType defaultPolicyType = PolicyType.COMMON;
    private static long defaultPolicyCode = 0L;

    @Autowired
    public void setSnowflakeIdGenerator(SnowflakeIdGenerator generator) {
        SnowflakeKeyGenerator.idGenerator = generator;
        logger.info("SnowflakeIdGenerator injected into SnowflakeKeyGenerator");
    }

    /**
     * 生成主键 - ShardingSphere 5.5.x 新方法
     */
    @Override
    public Collection<Comparable<?>> generateKeys(AlgorithmSQLContext context, int keyGenerateCount) {
        if (idGenerator == null) {
            logger.error("SnowflakeIdGenerator is not initialized");
            throw new IllegalStateException("SnowflakeIdGenerator is not initialized");
        }

        try {
            // 使用默认政策类型和代码生成ID
            long id = idGenerator.generateId(defaultPolicyType, defaultPolicyCode);
            logger.debug("Generated snowflake key: {}", id);
            return java.util.Collections.singletonList(id);
        } catch (Exception e) {
            logger.error("Failed to generate snowflake key", e);
            throw new RuntimeException("Failed to generate snowflake key", e);
        }
    }

    /**
     * 设置默认政策类型（用于配置初始化）
     */
    public static void setDefaultPolicyType(PolicyType policyType) {
        defaultPolicyType = policyType;
    }

    /**
     * 设置默认政策代码（用于配置初始化）
     */
    public static void setDefaultPolicyCode(long policyCode) {
        defaultPolicyCode = policyCode;
    }

    @Override
    public void init(Properties props) {
        logger.info("Initializing SnowflakeKeyGenerator with props: {}", props);
        // 可以从配置中读取默认参数
        if (props != null) {
            String policyTypeStr = props.getProperty("policy-type");
            if (policyTypeStr != null) {
                defaultPolicyType = PolicyType.fromName(policyTypeStr);
            }
            String policyCodeStr = props.getProperty("policy-code");
            if (policyCodeStr != null) {
                defaultPolicyCode = Long.parseLong(policyCodeStr);
            }
        }
    }

    @Override
    public String getType() {
        return "CUSTOM_SNOWFLAKE";
    }
}
