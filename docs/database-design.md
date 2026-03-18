# 数据库设计文档

> 本文档详细说明 VibeCoding001 项目的数据库表结构设计。

## 1. 数据库概览

### 1.1 数据库列表
| 数据库名 | 用途 | 字符集 |
|---------|------|--------|
| rule_engine | 规则引擎数据 | utf8mb4 |
| payment_db | 支付服务数据 | utf8mb4 |

### 1.2 表清单
| 表名 | 所属库 | 说明 |
|------|--------|------|
| payment_request | payment_db | 支付请求表 |
| rule | rule_engine | 规则表 |
| rule_history | rule_engine | 规则历史表 |
| rule_execution_log | rule_engine | 规则执行日志表 |
| external_api_config | rule_engine | 外部API配置表 |

---

## 2. 支付服务表

### 2.1 payment_request（支付请求表）

**表说明**: 存储所有支付请求的数据，包括请求信息、状态、处理结果等。

**表结构**:
```sql
CREATE TABLE IF NOT EXISTS payment_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    request_id VARCHAR(64) NOT NULL COMMENT '请求ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    idcard VARCHAR(18) NOT NULL COMMENT '身份证号',
    bankcard VARCHAR(32) NOT NULL COMMENT '银行卡号',
    amount DECIMAL(18, 2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED/TIMEOUT',
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
```

**字段说明**:

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 自增主键 |
| request_id | VARCHAR(64) | 是 | - | 请求唯一标识，格式：REQ_{时间戳}_{随机数} |
| name | VARCHAR(100) | 是 | - | 用户姓名 |
| idcard | VARCHAR(18) | 是 | - | 身份证号 |
| bankcard | VARCHAR(32) | 是 | - | 银行卡号 |
| amount | DECIMAL(18,2) | 是 | - | 支付金额 |
| status | VARCHAR(20) | 是 | PENDING | 支付状态 |
| batch_id | VARCHAR(64) | 否 | NULL | 批次ID，调用第三方时生成 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 记录创建时间 |
| submit_time | DATETIME | 否 | NULL | 提交给第三方的时间 |
| process_time | DATETIME | 否 | NULL | 处理完成时间 |
| error_msg | VARCHAR(500) | 否 | NULL | 错误信息 |
| retry_count | INT | 是 | 0 | 重试次数 |
| order_type | VARCHAR(20) | 是 | NORMAL | 订单类型：VIP或NORMAL |

**状态说明**:
| 状态值 | 说明 |
|--------|------|
| PENDING | 待处理，已入队等待批次处理 |
| PROCESSING | 处理中，已提交给第三方 |
| SUCCESS | 支付成功 |
| FAILED | 支付失败 |
| TIMEOUT | 处理超时 |

**索引说明**:
| 索引名 | 类型 | 字段 | 用途 |
|--------|------|------|------|
| uk_request_id | UNIQUE | request_id | 请求ID唯一性约束 |
| idx_status_create_time | INDEX | status, create_time | 按状态和时间查询 |
| idx_batch_id | INDEX | batch_id | 按批次查询 |
| idx_order_type_status | INDEX | order_type, status, create_time | 按订单类型和状态查询 |

---

## 3. 规则引擎表

### 3.1 rule（规则表）

**表说明**: 存储业务规则的定义，包括条件表达式和动作代码。

**表结构**:
```sql
CREATE TABLE IF NOT EXISTS `rule` (
    `id` VARCHAR(50) NOT NULL COMMENT '规则ID',
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `condition` TEXT NOT NULL COMMENT '规则条件（JSON格式）',
    `action` TEXT NOT NULL COMMENT '规则动作（Groovy代码）',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
    `version_comment` VARCHAR(255) DEFAULT NULL COMMENT '版本说明',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_priority` (`priority`),
    INDEX `idx_enabled` (`enabled`),
    INDEX `idx_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则表';
```

**字段说明**:

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | VARCHAR(50) | 是 | - | 规则唯一标识 |
| name | VARCHAR(100) | 是 | - | 规则名称 |
| condition | TEXT | 是 | - | 条件表达式，JSON格式 |
| action | TEXT | 是 | - | 动作代码，Groovy脚本 |
| priority | INT | 是 | 0 | 优先级，数值越大优先级越高 |
| enabled | TINYINT(1) | 是 | 1 | 是否启用 |
| version | INT | 是 | 1 | 版本号 |
| version_comment | VARCHAR(255) | 否 | NULL | 版本说明 |
| created_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 更新时间 |

**条件表达式示例**:
```json
{
  "type": "composite",
  "logic": "AND",
  "conditions": [
    {
      "type": "atomic",
      "field": "age",
      "operator": ">=",
      "value": 18
    },
    {
      "type": "atomic",
      "field": "status",
      "operator": "==",
      "value": "active"
    }
  ]
}
```

**动作代码示例**:
```groovy
return [
    result: "APPROVED",
    message: "审核通过",
    discount: context.amount * 0.1
]
```

---

### 3.2 rule_history（规则历史表）

**表说明**: 存储规则的历史版本，支持版本回滚。

**表结构**:
```sql
CREATE TABLE IF NOT EXISTS `rule_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录ID',
    `rule_id` VARCHAR(50) NOT NULL COMMENT '规则ID',
    `version` INT NOT NULL COMMENT '版本号',
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `condition` TEXT NOT NULL COMMENT '规则条件',
    `action` TEXT NOT NULL COMMENT '规则动作',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `version_comment` VARCHAR(255) DEFAULT NULL COMMENT '版本说明',
    `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_version` (`version`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则历史表';
```

**字段说明**:

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 自增主键 |
| rule_id | VARCHAR(50) | 是 | - | 规则ID |
| version | INT | 是 | - | 版本号 |
| name | VARCHAR(100) | 是 | - | 规则名称 |
| condition | TEXT | 是 | - | 规则条件 |
| action | TEXT | 是 | - | 规则动作 |
| priority | INT | 是 | 0 | 优先级 |
| enabled | TINYINT(1) | 是 | 1 | 是否启用 |
| version_comment | VARCHAR(255) | 否 | NULL | 版本说明 |
| created_by | VARCHAR(100) | 否 | NULL | 创建人 |
| created_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 创建时间 |

---

### 3.3 rule_execution_log（规则执行日志表）

**表说明**: 记录规则执行的详细日志，用于监控和审计。

**表结构**:
```sql
CREATE TABLE IF NOT EXISTS rule_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    rule_id VARCHAR(100) NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(200) COMMENT '规则名称',
    rule_version INT COMMENT '规则版本',
    `condition` TEXT COMMENT '规则条件',
    action TEXT COMMENT '规则动作',
    input_data TEXT COMMENT '输入数据(JSON格式)',
    output_data TEXT COMMENT '输出数据(JSON格式)',
    matched BOOLEAN DEFAULT FALSE COMMENT '是否匹配',
    executed BOOLEAN DEFAULT FALSE COMMENT '是否执行成功',
    execution_time BIGINT COMMENT '执行耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_rule_id (rule_id),
    INDEX idx_created_at (created_at),
    INDEX idx_rule_version (rule_id, rule_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则执行日志表';
```

**字段说明**:

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 自增主键 |
| rule_id | VARCHAR(100) | 是 | - | 规则ID |
| rule_name | VARCHAR(200) | 否 | NULL | 规则名称 |
| rule_version | INT | 否 | NULL | 规则版本 |
| condition | TEXT | 否 | NULL | 规则条件 |
| action | TEXT | 否 | NULL | 规则动作 |
| input_data | TEXT | 否 | NULL | 输入数据 |
| output_data | TEXT | 否 | NULL | 输出数据 |
| matched | BOOLEAN | 是 | FALSE | 条件是否匹配 |
| executed | BOOLEAN | 是 | FALSE | 动作是否执行成功 |
| execution_time | BIGINT | 否 | NULL | 执行耗时(ms) |
| error_message | TEXT | 否 | NULL | 错误信息 |
| created_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 创建时间 |

---

### 3.4 external_api_config（外部API配置表）

**表说明**: 存储外部API的配置信息，支持动态调用外部服务。

**表结构**:
```sql
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
    retry_times INT DEFAULT 3 COMMENT '重试次数',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    description TEXT COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部API配置表';
```

**字段说明**:

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| api_id | VARCHAR(100) | 是 | - | API唯一标识 |
| api_name | VARCHAR(200) | 是 | - | API名称 |
| api_url | VARCHAR(500) | 是 | - | API请求地址 |
| http_method | VARCHAR(10) | 是 | POST | HTTP方法 |
| request_template | TEXT | 否 | NULL | 请求体模板 |
| response_field | VARCHAR(200) | 否 | NULL | 响应字段路径 |
| success_condition | VARCHAR(500) | 否 | NULL | 成功条件表达式 |
| headers | TEXT | 否 | NULL | 请求头配置 |
| timeout | INT | 是 | 5000 | 超时时间(ms) |
| retry_times | INT | 是 | 3 | 重试次数 |
| enabled | BOOLEAN | 是 | TRUE | 是否启用 |
| description | TEXT | 否 | NULL | 描述 |
| created_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 是 | CURRENT_TIMESTAMP | 更新时间 |

---

## 4. 数据库关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                        payment_db 数据库                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────────────────────────────────────┐      │
│   │              payment_request                        │      │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │      │
│   │  │ request_id  │  │   status    │  │  batch_id   │ │      │
│   │  └─────────────┘  └─────────────┘  └─────────────┘ │      │
│   └─────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       rule_engine 数据库                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────┐      ┌─────────────────────┐         │
│   │        rule         │◄────►│    rule_history     │         │
│   │  ┌───────────────┐  │      │  ┌───────────────┐  │         │
│   │  │     id        │  │      │  │   rule_id     │  │         │
│   │  │   version     │  │      │  │   version     │  │         │
│   │  └───────────────┘  │      │  └───────────────┘  │         │
│   └─────────────────────┘      └─────────────────────┘         │
│                                                                 │
│   ┌─────────────────────────────────────────────────────┐      │
│   │           rule_execution_log                        │      │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │      │
│   │  │   rule_id   │  │   matched   │  │ created_at  │ │      │
│   │  └─────────────┘  └─────────────┘  └─────────────┘ │      │
│   └─────────────────────────────────────────────────────┘      │
│                                                                 │
│   ┌─────────────────────────────────────────────────────┐      │
│   │          external_api_config                        │      │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │      │
│   │  │   api_id    │  │  api_url    │  │   enabled   │ │      │
│   │  └─────────────┘  └─────────────┘  └─────────────┘ │      │
│   └─────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 初始化脚本

### 5.1 创建数据库
```sql
-- 创建支付数据库
CREATE DATABASE IF NOT EXISTS payment_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建规则引擎数据库
CREATE DATABASE IF NOT EXISTS rule_engine 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### 5.2 执行DDL脚本
```bash
# 支付服务表
mysql -u root -p payment_db < src/main/resources/db/payment_ddl.sql

# 规则引擎表
mysql -u root -p rule_engine < src/main/resources/sql/rule.sql
mysql -u root -p rule_engine < src/main/resources/sql/rule_execution_log.sql
mysql -u root -p rule_engine < src/main/resources/sql/external_api_config.sql
```

---

## 6. 维护说明

### 6.1 数据清理策略
| 表名 | 清理策略 | 保留时间 |
|------|---------|---------|
| payment_request | 归档历史数据 | 90天 |
| rule_execution_log | 定期清理 | 30天 |

### 6.2 索引优化建议
- 定期分析表：`ANALYZE TABLE table_name;`
- 查看执行计划：`EXPLAIN SELECT ...`
- 监控慢查询：开启慢查询日志

### 6.3 备份策略
- 全量备份：每天凌晨2点
- 增量备份：每4小时
- 保留周期：7天

---

**文档版本**: v1.0  
**最后更新**: 2026-03-18  
**维护人员**: VibeCoding Team
