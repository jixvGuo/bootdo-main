-- =============================================================
-- 勘察奖专家打分页：专家审核意见 + 主评意见（独立表，与淘汰评级活动表解耦）
-- 部署：在目标库执行本脚本一次
-- =============================================================

CREATE TABLE IF NOT EXISTS `ass_surver_expert_review_opinion` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`                VARCHAR(64)  NOT NULL COMMENT '任务 ID (publish_task_id)',
  `pro_id`                 INT          NOT NULL COMMENT '项目 ID',
  `pro_sub_type`           VARCHAR(32)  NOT NULL COMMENT 'contribution/design/software/standard/consulting',
  `expert_uid`             BIGINT       NOT NULL COMMENT '专家 sys_user.user_id',
  `audit_opinion`          VARCHAR(16)           DEFAULT NULL COMMENT 'agree | disagree，空=未选',
  `main_review_text`       TEXT                  DEFAULT NULL COMMENT '主评意见正文',
  `main_review_submitted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1=已提交主评(锁定填写流程，与淘汰确认提交无关)',
  `created`                DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `updated`                DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_pro_expert_rev` (`task_id`, `pro_id`, `expert_uid`),
  KEY `idx_task_expert_rev` (`task_id`, `expert_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勘察奖-专家审核意见/主评意见(不落快照)';
