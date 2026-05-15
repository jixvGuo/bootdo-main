package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.SurverExpertEliminateDO;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家淘汰评级 活动表 Service
 */
public interface SurverExpertEliminateService {

    SurverExpertEliminateDO get(Long id);

    List<SurverExpertEliminateDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertEliminateDO d);

    int update(SurverExpertEliminateDO d);

    int remove(Long id);

    SurverExpertEliminateDO getByUnique(String taskId, Integer proId, Long expertUid);

    /**
     * upsert：存在则更新 grade/remark，不存在则新增；返回受影响记录的 id
     */
    Long saveOrUpdateGrade(SurverExpertEliminateDO d);

    int batchSoftDeleteByTaskId(String taskId);

    // ============================================================
    // Phase B 新增 - 管理员淘汰管理
    // ============================================================

    java.util.List<java.util.Map<String, Object>> aggregateCandidates(String taskId, String proSubType, Long contactUserId);

    int updateEliminatedBySubType(String proSubType, Integer proId, Integer eliminated);

    int insertMinimalIfNotExists(String proSubType, Integer proId, Integer eliminated);

    java.util.List<java.util.Map<String, Object>> listConfirmedEliminated(String taskId);

    int countAssignedProjects(String taskId, Long uid);

    java.util.Map<String, Object> getProjectSnapshotInfo(Integer proId);

    java.util.Map<String, Object> findProInfoByProCode(String taskId, String proCode);

    java.util.List<java.util.Map<String, Object>> listExpertEvalDetail(String taskId, Long contactUserId);

    int countProInSurverContactScope(Integer proId, Long contactUserId);

    /**
     * 勘察奖小组联络人：当前任务下 surver_view_scope 可见的项目 id 列表（与项目列表 contactUserId 过滤同源）
     */
    java.util.List<java.lang.Integer> listProIdsVisibleToSurverContact(String taskId, Long contactUserId);

    /**
     * 专家侧：仅本人淘汰评级及评语（「下载淘汰评语」导出）
     */
    java.util.List<java.util.Map<String, Object>> listMyExpertGroupEliminateDetail(String taskId, String proSubType, Long viewerUid);
}
