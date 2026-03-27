-- ============================================
-- ShardingSphere 分表 DDL
-- payment_request 表分为 16 张表：payment_request_0 ~ payment_request_15
-- 分片键：policy_code (policy_code % 16 决定分到哪张表)
-- ============================================

-- 创建分表存储过程
DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS CreatePaymentRequestTables()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE table_name VARCHAR(50);
    
    WHILE i < 16 DO
        SET table_name = CONCAT('payment_request_', i);
        
        SET @sql = CONCAT('
            CREATE TABLE IF NOT EXISTS ', table_name, ' (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT ''自增ID'',
                snowflake_id BIGINT NOT NULL COMMENT ''雪花算法生成的唯一ID'',
                request_id VARCHAR(64) NOT NULL COMMENT ''请求ID'',
                name VARCHAR(100) NOT NULL COMMENT ''姓名'',
                idcard VARCHAR(18) NOT NULL COMMENT ''身份证号'',
                bankcard VARCHAR(32) NOT NULL COMMENT ''银行卡号'',
                amount DECIMAL(18, 2) NOT NULL COMMENT ''支付金额'',
                status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''状态'',
                batch_id VARCHAR(64) NULL COMMENT ''批次ID'',
                policy_type VARCHAR(20) NOT NULL DEFAULT ''common'' COMMENT ''政策类型'',
                policy_code BIGINT NOT NULL DEFAULT 0 COMMENT ''政策编号(分片键)'',
                create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
                submit_time DATETIME NULL COMMENT ''提交第三方时间'',
                process_time DATETIME NULL COMMENT ''处理完成时间'',
                error_msg VARCHAR(500) NULL COMMENT ''错误信息'',
                retry_count INT NOT NULL DEFAULT 0 COMMENT ''重试次数'',
                order_type VARCHAR(20) NOT NULL DEFAULT ''NORMAL'' COMMENT ''订单类型'',

                UNIQUE KEY uk_snowflake_id (snowflake_id),
                UNIQUE KEY uk_request_id (request_id),
                INDEX idx_status_create_time (status, create_time),
                INDEX idx_batch_id (batch_id),
                INDEX idx_order_type_status (order_type, status, create_time),
                INDEX idx_policy_type_code (policy_type, policy_code),
                INDEX idx_snowflake_time (snowflake_id, create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''支付请求表分表 ', i, '''
        ');
        
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        
        SET i = i + 1;
    END WHILE;
END$$

DELIMITER ;

-- 执行存储过程创建所有分表
CALL CreatePaymentRequestTables();

-- 删除存储过程
DROP PROCEDURE IF EXISTS CreatePaymentRequestTables();

-- ============================================
-- 手动创建单表示例（如果不使用存储过程）
-- ============================================

-- payment_request_0
CREATE TABLE IF NOT EXISTS payment_request_0 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    snowflake_id BIGINT NOT NULL COMMENT '雪花算法生成的唯一ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    idcard VARCHAR(18) NOT NULL COMMENT '身份证号',
    bankcard VARCHAR(32) NOT NULL COMMENT '银行卡号',
    amount DECIMAL(18, 2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    batch_id VARCHAR(64) NULL COMMENT '批次ID（提交第三方时生成）',
    policy_type VARCHAR(20) NOT NULL DEFAULT 'common' COMMENT '政策类型：common/old_man/edu/live',
    policy_code BIGINT NOT NULL DEFAULT 0 COMMENT '政策编号(分片键)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    submit_time DATETIME NULL COMMENT '提交第三方时间',
    process_time DATETIME NULL COMMENT '处理完成时间',
    error_msg VARCHAR(500) NULL COMMENT '错误信息',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：VIP/NORMAL',

    UNIQUE KEY uk_snowflake_id (snowflake_id),
    UNIQUE KEY uk_request_id (request_id),
    INDEX idx_status_create_time (status, create_time),
    INDEX idx_batch_id (batch_id),
    INDEX idx_order_type_status (order_type, status, create_time),
    INDEX idx_policy_type_code (policy_type, policy_code),
    INDEX idx_snowflake_time (snowflake_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表0';

-- 注意：实际使用时需要创建 payment_request_1 到 payment_request_15 共16张表
-- 可以使用上面的存储过程批量创建，或者手动执行16次CREATE TABLE语句
