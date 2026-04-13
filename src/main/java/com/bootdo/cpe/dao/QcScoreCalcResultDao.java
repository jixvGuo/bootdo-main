package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcScoreCalcResultDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * QC评分计算汇总结果数据访问层
 * 对应表：ass_qc_score_calc_result
 * 存储每个项目在某阶段的平均分计算汇总，支持幂等 upsert
 */
@Mapper
public interface QcScoreCalcResultDao {

    QcScoreCalcResultDO get(Integer id);

    List<QcScoreCalcResultDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcScoreCalcResultDO result);

    /** INSERT ... ON DUPLICATE KEY UPDATE — 唯一键 (task_id,pro_id,phase) 幂等更新 */
    int upsert(QcScoreCalcResultDO result);

    int update(QcScoreCalcResultDO result);

    int remove(Integer id);

    int batchRemove(Integer[] ids);
}
