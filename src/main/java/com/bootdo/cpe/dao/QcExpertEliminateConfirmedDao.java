package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

/**
 * QC专家淘汰确认快照表
 */
@Mapper
public interface QcExpertEliminateConfirmedDao {

    QcExpertEliminateConfirmedDO get(Integer id);

    List<QcExpertEliminateConfirmedDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcExpertEliminateConfirmedDO record);

    int batchSaveFromEliminate(Map<String, Object> params);
}
