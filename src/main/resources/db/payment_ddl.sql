-- 支付请求表
CREATE TABLE IF NOT EXISTS payment_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    idcard VARCHAR(18) NOT NULL COMMENT '身份证号',
    bankcard VARCHAR(32) NOT NULL COMMENT '银行卡号',
    amount DECIMAL(18, 2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    batch_id VARCHAR(64) NULL COMMENT '批次ID（提交第三方时生成）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    submit_time DATETIME NULL COMMENT '提交第三方时间',
    process_time DATETIME NULL COMMENT '处理完成时间',
    error_msg VARCHAR(500) NULL COMMENT '错误信息',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：VIP/NORMAL',

    UNIQUE KEY uk_request_id (request_id),
    INDEX idx_status_create_time (status, create_time),
    INDEX idx_batch_id (batch_id),
    INDEX idx_order_type_status (order_type, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付请求表';

-- 已有表的变更（如果表已存在，执行以下ALTER语句）
-- ALTER TABLE payment_request ADD COLUMN order_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：VIP/NORMAL' AFTER retry_count;
-- ALTER TABLE payment_request ADD INDEX idx_order_type_status (order_type, status, create_time);
