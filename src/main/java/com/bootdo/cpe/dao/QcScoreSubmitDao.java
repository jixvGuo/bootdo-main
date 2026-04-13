package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcScoreSubmitDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * QC专家打分提交快照数据访问层
 * 对应表：ass_qc_score_submit
 * 专家 scoreOver=1 时写入快照，唯一键 (task_id, pro_id, expert_uid, phase)，天然幂等
 */
@Mapper
public interface QcScoreSubmitDao {

    QcScoreSubmitDO get(Integer id);

    List<QcScoreSubmitDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    /** INSERT IGNORE — 唯一键冲突时跳过，保证幂等 */
    int saveIgnore(QcScoreSubmitDO submit);

    int remove(Integer id);

    /** 按 taskId + proId + phase 删除（重算前清理旧数据用） */
    int deleteByProAndPhase(Map<String, Object> params);
}
