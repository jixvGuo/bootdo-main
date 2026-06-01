-- 勘察奖项目列表：「是否有查新」持久化（仅允许 是 / 否，存于项目主表）
-- 执行一次即可；若列已存在会报错，可忽略或先 SHOW COLUMNS 检查

ALTER TABLE `ass_award_enterprise_project`
  ADD COLUMN `ext_surver_novelty` VARCHAR(8) NULL DEFAULT NULL
  COMMENT '勘察奖-是否有查新(是/否)，由导出Excel回填或导入形式审查结果更新'
  AFTER `declare_account`;
