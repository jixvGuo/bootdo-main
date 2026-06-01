package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.SurverExpertScoringDO;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖专家打分Service
 */
public interface SurverExpertScoringService {

    SurverExpertScoringDO get(Long id);

    SurverExpertScoringDO getByTaskProExpert(String taskId, Integer proId, Long expertUid);

    List<SurverExpertScoringDO> list(Map<String, Object> params);

    int count(Map<String, Object> params);

    int save(SurverExpertScoringDO scoring);

    int update(SurverExpertScoringDO scoring);

    int remove(Long id);

    int batchRemove(Long[] ids);

    /**
     * 确认打分结果
     */
    int confirmScoring(String taskId, Long expertUid);

    /**
     * 检查是否已确认
     */
    boolean isConfirmed(String taskId, Long expertUid);

    /**
     * 重置打分确认状态（管理员驳回专家的打分确认）
     */
    int resetConfirmStatus(String taskId, Long expertUid);

    /**
     * 导出分数查询：获取项目+专家打分+专家分组信息
     */
    List<Map<String, Object>> listForExport(Map<String, Object> params);
}