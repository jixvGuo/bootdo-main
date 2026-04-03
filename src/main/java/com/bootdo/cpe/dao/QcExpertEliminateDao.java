package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcExpertEliminateDO;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * QC专家淘汰记录表
 */
@Mapper
public interface QcExpertEliminateDao {

    QcExpertEliminateDO get(Integer id);

    List<QcExpertEliminateDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcExpertEliminateDO qcExpertEliminate);

    int update(QcExpertEliminateDO qcExpertEliminate);

    int remove(Integer id);

    int batchSoftDeleteByTaskId(Map<String, Object> params);
}
