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

    int batchSoftDeleteByTaskId(String taskId);

    /**
     * 删除当前专家在指定任务下的淘汰名单快照
     */
    int deleteQrByExpertAndTask(Long expertUid, String taskId);

    /**
     * 将当前专家在指定任务下的有效淘汰名单快照保存到 ass_qc_expert_eliminate_qr
     */
    int saveCurrentEliminateListToQr(Long expertUid, String taskId);
}
