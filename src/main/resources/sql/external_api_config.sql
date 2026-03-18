-- 外部API配置表
CREATE TABLE IF NOT EXISTS external_api_config (
    api_id VARCHAR(100) PRIMARY KEY COMMENT 'API ID',
    api_name VARCHAR(200) NOT NULL COMMENT 'API名称',
    api_url VARCHAR(500) NOT NULL COMMENT 'API地址',
    http_method VARCHAR(10) DEFAULT 'POST' COMMENT 'HTTP方法(GET/POST/PUT/DELETE)',
    request_template TEXT COMMENT '请求体模板(JSON格式，支持${变量}占位符)',
    response_field VARCHAR(200) COMMENT '响应字段路径(如:data.result)',
    success_condition VARCHAR(500) COMMENT '成功条件表达式(如:response == true)',
    headers TEXT COMMENT '请求头(JSON格式，支持${变量}占位符)',
    timeout INT DEFAULT 5000 COMMENT '超时时间(毫秒)',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    description TEXT COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部API配置表';
