package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcPresentScoreDO;

import java.util.List;
import java.util.Map;

/**
 * QC奖发布分（第二次打分）Service
 */
public interface QcPresentScoreService {

    QcPresentScoreDO get(Integer id);

    List<QcPresentScoreDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcPresentScoreDO qcPresentScore);

    int update(QcPresentScoreDO qcPresentScore);

    int remove(Integer id);

    int batchRemove(Integer[] ids);
}
