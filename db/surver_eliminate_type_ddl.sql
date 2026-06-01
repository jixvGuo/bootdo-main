-- 勘察奖：淘汰类型（评级淘汰 / 打分淘汰）
-- eliminated 仍为 0/1 总开关；eliminate_type 仅 eliminated=1 时有意义：rating | score

ALTER TABLE `ass_surver_excellent_apply_table_info`
    ADD COLUMN `eliminate_type` VARCHAR(16) DEFAULT NULL COMMENT '淘汰类型: rating=评级淘汰, score=打分淘汰' AFTER `eliminated`;

ALTER TABLE `ass_surver_design_apply_table_info`
    ADD COLUMN `eliminate_type` VARCHAR(16) DEFAULT NULL COMMENT '淘汰类型: rating=评级淘汰, score=打分淘汰' AFTER `eliminated`;

ALTER TABLE `ass_surver_soft_apply_table_info`
    ADD COLUMN `eliminate_type` VARCHAR(16) DEFAULT NULL COMMENT '淘汰类型: rating=评级淘汰, score=打分淘汰' AFTER `eliminated`;

ALTER TABLE `ass_surver_standard_apply_table_info`
    ADD COLUMN `eliminate_type` VARCHAR(16) DEFAULT NULL COMMENT '淘汰类型: rating=评级淘汰, score=打分淘汰' AFTER `eliminated`;

-- 历史已淘汰数据视为评级淘汰
UPDATE `ass_surver_excellent_apply_table_info` SET `eliminate_type` = 'rating' WHERE `eliminated` = 1 AND (`eliminate_type` IS NULL OR TRIM(`eliminate_type`) = '');
UPDATE `ass_surver_design_apply_table_info`    SET `eliminate_type` = 'rating' WHERE `eliminated` = 1 AND (`eliminate_type` IS NULL OR TRIM(`eliminate_type`) = '');
UPDATE `ass_surver_soft_apply_table_info`      SET `eliminate_type` = 'rating' WHERE `eliminated` = 1 AND (`eliminate_type` IS NULL OR TRIM(`eliminate_type`) = '');
UPDATE `ass_surver_standard_apply_table_info`  SET `eliminate_type` = 'rating' WHERE `eliminated` = 1 AND (`eliminate_type` IS NULL OR TRIM(`eliminate_type`) = '');
