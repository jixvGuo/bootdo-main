package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcScoreCalcResultDO;

import java.util.List;
import java.util.Map;

/**
 * QC评分计算汇总结果服务接口
 * 提供对 ass_qc_score_calc_result 表的 CRUD，并支持幂等 upsert
 */
public interface QcScoreCalcResultService {

    QcScoreCalcResultDO get(Integer id);

    List<QcScoreCalcResultDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    /** 普通插入 */
    int save(QcScoreCalcResultDO result);

    /** 幂等写入：存在则更新平均分及人数，对应 INSERT ... ON DUPLICATE KEY UPDATE */
    int upsert(QcScoreCalcResultDO result);

    int update(QcScoreCalcResultDO result);

    int remove(Integer id);

    int batchRemove(Integer[] ids);
}
