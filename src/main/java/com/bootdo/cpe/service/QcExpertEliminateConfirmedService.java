package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcExpertEliminateConfirmedDO;

import java.util.List;
import java.util.Map;

/**
 * QC专家淘汰确认快照表
 */
public interface QcExpertEliminateConfirmedService {

    QcExpertEliminateConfirmedDO get(Integer id);

    List<QcExpertEliminateConfirmedDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(QcExpertEliminateConfirmedDO record);

    /**
     * 批量从 ass_qc_expert_eliminate 表复制有效记录到快照表
     * @param expertUid 专家UID
     * @param taskId 任务ID
     * @return 插入条数
     */
    int batchSaveFromEliminate(Long expertUid, String taskId);
}
