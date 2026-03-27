-- ============================================
-- ShardingSphere 分表 DDL - 手动创建版本
-- payment_request 表分为 16 张表：payment_request_0 ~ payment_request_15
-- 分片键：policy_code (policy_code % 16 决定分到哪张表)
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

-- payment_request_1
CREATE TABLE IF NOT EXISTS payment_request_1 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表1';

-- payment_request_2
CREATE TABLE IF NOT EXISTS payment_request_2 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表2';

-- payment_request_3
CREATE TABLE IF NOT EXISTS payment_request_3 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表3';

-- payment_request_4
CREATE TABLE IF NOT EXISTS payment_request_4 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表4';

-- payment_request_5
CREATE TABLE IF NOT EXISTS payment_request_5 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表5';

-- payment_request_6
CREATE TABLE IF NOT EXISTS payment_request_6 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表6';

-- payment_request_7
CREATE TABLE IF NOT EXISTS payment_request_7 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表7';

-- payment_request_8
CREATE TABLE IF NOT EXISTS payment_request_8 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表8';

-- payment_request_9
CREATE TABLE IF NOT EXISTS payment_request_9 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表9';

-- payment_request_10
CREATE TABLE IF NOT EXISTS payment_request_10 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表10';

-- payment_request_11
CREATE TABLE IF NOT EXISTS payment_request_11 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表11';

-- payment_request_12
CREATE TABLE IF NOT EXISTS payment_request_12 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表12';

-- payment_request_13
CREATE TABLE IF NOT EXISTS payment_request_13 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表13';

-- payment_request_14
CREATE TABLE IF NOT EXISTS payment_request_14 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表14';

-- payment_request_15
CREATE TABLE IF NOT EXISTS payment_request_15 (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表分表15';

-- ============================================
-- 验证脚本
-- ============================================

-- 查看所有分表
-- SHOW TABLES LIKE 'payment_request_%';

-- 查看各分表的记录数
-- SELECT 'payment_request_0' as table_name, COUNT(*) as count FROM payment_request_0
-- UNION ALL SELECT 'payment_request_1', COUNT(*) FROM payment_request_1
-- UNION ALL SELECT 'payment_request_2', COUNT(*) FROM payment_request_2
-- ... 继续到 payment_request_15
