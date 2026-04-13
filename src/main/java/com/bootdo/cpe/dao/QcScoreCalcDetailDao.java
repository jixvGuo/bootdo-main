package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcScoreCalcDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * QC评分计算明细数据访问层
 * 对应表：ass_qc_score_calc_detail
 * 记录每个专家在计算圈内的原始分、回避、参与计算、剔除最高/最低等明细
 */
@Mapper
public interface QcScoreCalcDetailDao {

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
