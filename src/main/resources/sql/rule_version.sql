USE rule_engine;

ALTER TABLE `rule` 
ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '版本号',
ADD COLUMN `version_comment` VARCHAR(255) DEFAULT NULL COMMENT '版本说明',
ADD INDEX `idx_version` (`version`);

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