-- =============================================================
-- 专家分组管理 DDL（与现有"分组管理 / ass_qc_group"不是同一功能点）
-- 模块：勘察奖任务管理 -> 项目列表（四个子 tab）-> 专家分组管理
-- =============================================================

-- 1) 专家分组主表
CREATE TABLE IF NOT EXISTS `ass_award_expert_group` (
  `taskid`  VARCHAR(64)  NOT NULL COMMENT '任务 ID',
  `groupid` INT          NOT NULL COMMENT '分组 ID（任务内自增，由后端分配）',
  `name`    VARCHAR(255) NOT NULL COMMENT '专家分组名称',
  PRIMARY KEY (`taskid`, `groupid`),
  UNIQUE KEY `uk_task_name` (`taskid`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家分组（独立于 ass_qc_group）';

-- 2) 课题与专家分组的关联表（每个课题在同一任务内只能属于一个专家分组）
CREATE TABLE IF NOT EXISTS `ass_award_pro_expert_group` (
  `taskid`  VARCHAR(64) NOT NULL COMMENT '任务 ID',
  `proid`   INT         NOT NULL COMMENT '课题 ID',
  `groupid` INT         NOT NULL COMMENT '专家分组 ID',
  PRIMARY KEY (`taskid`, `proid`),
  KEY `idx_task_group` (`taskid`, `groupid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课题-专家分组 关联';

-- =============================================================
-- 新增：勘察奖"小组联络人"角色 + 视图绑定数据说明
-- 用途：为勘察奖专业组管理新增一种"按专业组维度限制可见性"的角色，
--      与 QC 角色 72(QC奖协会外聘人员) 的实现思路一致：
--        · 协会领导/勘察奖协会联系人 通过页面"小组联络人绑定"按钮设置
--          某个登录用户(role=勘察奖小组联络人) 能看到哪些专家组；
--        · 该角色登录进入 /cpe/suverProcess/toSurverMajorGroupAdmin 时，
--          仅展示绑定到自己名下的 group，不展示"重新分组"和"形审专家绑定"按钮。
-- 数据存放：复用 add_special_info(ExpertGroupDO)，
--          pro_type='surver_view_scope'，user_id=登录用户ID，group_name=可见的专家组名
-- =============================================================

-- 3) 新角色: 勘察奖小组联络人 （部署后请把 Constant.ROLE_SURVER_GROUP_CONTACT_ID 占位 -75L 替换为真实 role_id）
-- 注意: 不同环境 sys_role 表结构可能略有差异, 下面 INSERT 仅给出最小必填字段 + 名称, 字段缺省值/审计字段请按本项目实际表结构补齐。
INSERT INTO `sys_role` (`role_name`, `role_sign`, `remark`)
VALUES ('勘察奖小组联络人', 'survey_group_contact', '只能看到被绑定的勘察奖专家组及其专家信息（专业组管理页面）');
-- 拿到自增 role_id 后:
--   1) 更新 com.bootdo.common.config.Constant.ROLE_SURVER_GROUP_CONTACT_ID = <新ID>L
--   2) 在 sys_role_menu / sys_user_role 中按需绑定菜单与用户

-- ----- 3.1) 角色 86 菜单/用户授权脚本（请按需要调整 role_id 与 user_id） -----
-- 思路: 把"勘察奖协会外聘人员"(role 75)的菜单复制给 86, 按钮可见性已经在前端
--       通过 th:if + 权限标志控制 (canManageContactBinding / isSurverGroupContactRole),
--       因此这里只保证菜单能点进去, 不需要再剔除单独菜单.
-- 复制 75 角色菜单到 86, 已存在的不重复插入
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 86, rm.menu_id
FROM sys_role_menu rm
WHERE rm.role_id = 75
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu x WHERE x.role_id = 86 AND x.menu_id = rm.menu_id
  );

-- 给具体用户分配 86 角色 (把 <USER_ID> 替换为目标 sys_user.user_id)
-- INSERT INTO sys_user_role (user_id, role_id)
-- SELECT <USER_ID>, 86
-- WHERE NOT EXISTS (
--     SELECT 1 FROM sys_user_role WHERE user_id = <USER_ID> AND role_id = 86
-- );

-- 校验: 查看哪些用户当前拥有 86 角色
-- SELECT u.user_id, u.username, u.name FROM sys_user u
-- JOIN sys_user_role ur ON ur.user_id = u.user_id
-- WHERE ur.role_id = 86;

-- =============================================================
-- Phase A: 勘察奖"专家淘汰"功能 - 表结构
-- 流程:
--   1) 专家在"淘汰打分"页面对其负责的项目逐个评等级 A/B/C/D, 写入 ass_surver_expert_eliminate
--   2) 专家点击"确认淘汰名单"提交后, 数据复制到快照表 ass_surver_expert_eliminate_confirmed,
--      add_special_info.eliminate_over 置 1, 之后专家无法修改自己的等级
--   3) 协会管理员在"淘汰管理"弹窗中查看汇总(各专家对每个项目的评级), 手动点"淘汰"按钮,
--      把对应申报子表(4 张)中的 eliminated 字段置 1; 列表中"淘汰状态"列读该字段
-- 与 QC 区别: QC 用 reason(text), 勘察奖用 grade(A/B/C/D); QC 没有"管理员二次决定", 勘察奖有
-- =============================================================

-- 4) 专家淘汰活动表 (允许专家在确认前反复修改)
CREATE TABLE IF NOT EXISTS `ass_surver_expert_eliminate` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`      VARCHAR(64)  NOT NULL COMMENT '任务 ID',
  `pro_id`       INT          NOT NULL COMMENT '项目 ID (与申报子表 pro_id 一致)',
  `pro_sub_type` VARCHAR(32)  NOT NULL COMMENT '项目类别: contribution/design/software/standard',
  `pro_code`     VARCHAR(64)           DEFAULT NULL COMMENT '申报号(冗余,导出/展示用)',
  `topic_name`   VARCHAR(255)          DEFAULT NULL COMMENT '项目名称(冗余)',
  `company_name` VARCHAR(255)          DEFAULT NULL COMMENT '申报单位(冗余)',
  `group_name`   VARCHAR(128)          DEFAULT NULL COMMENT '专家组名(冗余)',
  `expert_uid`   BIGINT       NOT NULL COMMENT '专家 sys_user.user_id',
  `expert_name`  VARCHAR(64)           DEFAULT NULL COMMENT '专家姓名(冗余)',
  `grade`        VARCHAR(2)            DEFAULT NULL COMMENT '评级 A/B/C/D, 空=未评',
  `remark`       VARCHAR(512)          DEFAULT NULL COMMENT '专家备注(可选)',
  `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
  `created`      DATETIME              DEFAULT NULL,
  `updated`      DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_pro_expert` (`task_id`, `pro_id`, `expert_uid`),
  KEY `idx_task_expert` (`task_id`, `expert_uid`),
  KEY `idx_task_pro` (`task_id`, `pro_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勘察奖-专家淘汰评级活动表';

-- 5) 专家淘汰快照表 (一次性, 提交后只读)
CREATE TABLE IF NOT EXISTS `ass_surver_expert_eliminate_confirmed` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `task_id`      VARCHAR(64)  NOT NULL,
  `pro_id`       INT          NOT NULL,
  `pro_sub_type` VARCHAR(32)  NOT NULL,
  `pro_code`     VARCHAR(64)           DEFAULT NULL,
  `topic_name`   VARCHAR(255)          DEFAULT NULL,
  `company_name` VARCHAR(255)          DEFAULT NULL,
  `group_name`   VARCHAR(128)          DEFAULT NULL,
  `expert_uid`   BIGINT       NOT NULL,
  `expert_name`  VARCHAR(64)           DEFAULT NULL,
  `grade`        VARCHAR(2)            DEFAULT NULL,
  `remark`       VARCHAR(512)          DEFAULT NULL,    
  `confirmed_at` DATETIME              DEFAULT NULL COMMENT '专家确认提交时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_pro_expert_c` (`task_id`, `pro_id`, `expert_uid`),
  KEY `idx_task_expert_c` (`task_id`, `expert_uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勘察奖-专家淘汰评级快照表(确认后冻结)';

-- 6) 在 4 张申报子表追加 eliminated 字段, 表示"管理员最终淘汰决策"
--    0=未淘汰, 1=已淘汰; 默认 0; 仅由协会管理员通过"淘汰管理"弹窗手动设置
ALTER TABLE `ass_surver_excellent_apply_table_info` ADD COLUMN `eliminated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '管理员淘汰标记(0未淘汰/1已淘汰)';
ALTER TABLE `ass_surver_design_apply_table_info`    ADD COLUMN `eliminated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '管理员淘汰标记(0未淘汰/1已淘汰)';
ALTER TABLE `ass_surver_soft_apply_table_info`      ADD COLUMN `eliminated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '管理员淘汰标记(0未淘汰/1已淘汰)';
ALTER TABLE `ass_surver_standard_apply_table_info`  ADD COLUMN `eliminated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '管理员淘汰标记(0未淘汰/1已淘汰)';
-- 注: 如果数据库不支持 IF NOT EXISTS 形式 ADD COLUMN, 重复执行会报"列已存在"错误; 可忽略或先 SHOW COLUMNS 检查

-- =============================================================
-- Phase A 附加: 勘察奖"专家回避"表 (1:1 镜像 ass_qc_expert_avoidance)
-- 字段语义和 QC 完全一致, 通过 task_id 天然区分奖项, 不与 QC 表数据混淆
-- =============================================================
CREATE TABLE IF NOT EXISTS `ass_surver_expert_avoidance` (
  `id`                INT          NOT NULL AUTO_INCREMENT,
  `task_id`           VARCHAR(64)  NOT NULL COMMENT '任务 ID',
  `pro_id`            INT          NOT NULL COMMENT '项目 ID(对应 4 张申报子表 pro_id)',
  `expert_user_id`    INT          NOT NULL COMMENT '专家 sys_user.user_id',
  `avoidance_type`    VARCHAR(16)  NOT NULL DEFAULT 'manual' COMMENT 'auto | manual',
  `avoidance_reason`  VARCHAR(512)          DEFAULT NULL,
  `created_by`        INT                   DEFAULT NULL COMMENT '操作人 user_id',
  `created`           DATETIME              DEFAULT CURRENT_TIMESTAMP,
  `deleted`           TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_task_expert` (`task_id`, `expert_user_id`),
  KEY `idx_task_pro` (`task_id`, `pro_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勘察奖-专家回避记录表';
