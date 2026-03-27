CREATE DATABASE IF NOT EXISTS rule_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rule_engine;

CREATE TABLE IF NOT EXISTS `rule` (
    `id` VARCHAR(50) NOT NULL COMMENT '规则ID',
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `condition` TEXT NOT NULL COMMENT '规则条件',
    `action` TEXT NOT NULL COMMENT '规则动作',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_priority` (`priority`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则表';