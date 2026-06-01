-- =============================================================
-- 勘察奖专家打分功能 DDL
-- 用途：专家对四个子奖项的项目进行打分
-- =============================================================

-- 1) 专家打分明细表
CREATE TABLE IF NOT EXISTS `ass_surver_expert_scoring` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`               VARCHAR(64)  NOT NULL COMMENT '任务 ID',
  `pro_id`                INT          NOT NULL COMMENT '项目 ID',
  `pro_sub_type`          VARCHAR(32)  NOT NULL COMMENT '项目类别: contribution/design/software/standard',
  `expert_uid`            BIGINT       NOT NULL COMMENT '专家 sys_user.user_id',

  -- 勘察项目、标准设计项目、软件项目共用字段
  `technical_level`       INT          DEFAULT NULL COMMENT '技术水平',
  `technical_difficulty`  INT          DEFAULT NULL COMMENT '技术难度',
  `technical_innovation`  INT          DEFAULT NULL COMMENT '技术创新',
  `economic_benefit`      INT          DEFAULT NULL COMMENT '经济效益',
  `material_quality`      INT          DEFAULT NULL COMMENT '申报材料质量',

  -- 设计项目专用字段
  `overall_technical_level` INT        DEFAULT NULL COMMENT '总体工艺技术水平(设计)',
  `difficulty_innovation`   INT        DEFAULT NULL COMMENT '技术难度与创新(设计)',
  `digital_design_level`    INT        DEFAULT NULL COMMENT '数字化设计水平(设计)',
  `environment_safety`      INT        DEFAULT NULL COMMENT '环境保护安全卫生(设计)',
  `design_quality`          INT        DEFAULT NULL COMMENT '设计质量(设计)',
  `energy_saving`           INT        DEFAULT NULL COMMENT '节能降耗经济效益社会效益(设计)',
  `green_construction`      INT        DEFAULT NULL COMMENT '绿色建造与新能源利用(设计)',

  -- 标准设计和软件项目专用字段
  `promotability`         INT          DEFAULT NULL COMMENT '可推广应用性(标准/软件)',

  -- 总分
  `total_score`           INT          DEFAULT NULL COMMENT '总分',

  -- 主评意见
  `opinion_grade`         VARCHAR(16)  DEFAULT NULL COMMENT '主评等级: 不评/缓评/一等/二等/三等',
  `opinion_text`          TEXT         DEFAULT NULL COMMENT '主评意见文本',

  -- 状态
  `confirmed`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已确认(0未确认/1已确认)',
  `confirmed_at`          DATETIME     DEFAULT NULL COMMENT '确认时间',

  -- 审计字段
  `created`               DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated`               DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`               TINYINT(1)   NOT NULL DEFAULT 0,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_pro_expert` (`task_id`, `pro_id`, `expert_uid`),
  KEY `idx_task_expert` (`task_id`, `expert_uid`),
  KEY `idx_task_pro` (`task_id`, `pro_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勘察奖-专家打分明细表';