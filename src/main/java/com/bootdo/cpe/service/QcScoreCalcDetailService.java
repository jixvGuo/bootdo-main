package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcScoreCalcDetailDO;

import java.util.List;
import java.util.Map;

/**
 * QC评分计算明细服务接口
 * 提供对 ass_qc_score_calc_detail 表的 CRUD 及批量操作
 */
public interface QcScoreCalcDetailService {

    QcScoreCalcDetailDO get(Integer id);

    List<QcScoreCalcDetailDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    /** 写入单条明细 */
    int save(QcScoreCalcDetailDO detail);

    /** 批量写入明细（一次计算对应多条） */
    int batchSave(List<QcScoreCalcDetailDO> list);

    int update(QcScoreCalcDetailDO detail);

    int remove(Integer id);

    int batchRemove(Integer[] ids);

    /** 重算前清理某次计算结果的旧明细 */
    int deleteByResultId(Integer resultId);
}
