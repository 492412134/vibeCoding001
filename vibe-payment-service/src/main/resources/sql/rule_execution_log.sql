-- 规则执行日志表
CREATE TABLE IF NOT EXISTS rule_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    rule_id VARCHAR(100) NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(200) COMMENT '规则名称',
    `condition` TEXT COMMENT '规则条件',
    action TEXT COMMENT '规则动作',
    input_data TEXT COMMENT '输入数据(JSON格式)',
    output_data TEXT COMMENT '输出数据(JSON格式)',
    executed BOOLEAN DEFAULT FALSE COMMENT '是否执行成功',
    execution_time BIGINT COMMENT '执行耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则执行日志表';
