package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcExpertEliminateDO;

import java.util.List;
import java.util.Map;

/**
 * QC专家淘汰记录表
 */
public interface QcExpertEliminateService {

    QcExpertEliminateDO get(Integer id);

    List<QcExpertEliminateDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcExpertEliminateDO qcExpertEliminate);

    int update(QcExpertEliminateDO qcExpertEliminate);

    int remove(Integer id);
}
