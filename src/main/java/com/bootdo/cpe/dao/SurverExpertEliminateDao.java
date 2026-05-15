package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.SurverExpertEliminateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家淘汰评级 活动表 DAO
 */
@Mapper
public interface SurverExpertEliminateDao {

    SurverExpertEliminateDO get(Long id);

    List<SurverExpertEliminateDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertEliminateDO d);

    int update(SurverExpertEliminateDO d);

    int remove(Long id);

    /**
     * 按 task_id + pro_id + expert_uid 查重；存在则返回，不存在返回 null
     */
    SurverExpertEliminateDO getByUnique(@Param("taskId") String taskId,
                                        @Param("proId") Integer proId,
                                        @Param("expertUid") Long expertUid);

    /**
     * 整任务批量软删（撤销已确认时使用，谨慎调用）
     */
    int batchSoftDeleteByTaskId(@Param("taskId") String taskId);

    // ============================================================
    // Phase B 新增 - 管理员淘汰管理 弹窗用
    // ============================================================

    /**
     * 候选淘汰池：按 task+pro 聚合 + 各等级计数 + 专家评级拼接 + 当前 eliminated 状态
     * @return rows: { taskId, proId, proSubType, proCode, topicName, companyName, groupName,
     *               gradeA, gradeB, gradeC, gradeD, gradeEmpty, totalRows, expertGrades, eliminated }
     */
    List<Map<String, Object>> aggregateCandidates(@Param("taskId") String taskId,
                                                  @Param("proSubType") String proSubType,
                                                  @Param("contactUserId") Long contactUserId);

    /**
     * 路由更新对应申报子表的 eliminated 字段（管理员持久化"淘汰状态"）
     */
    int updateEliminatedBySubType(@Param("proSubType") String proSubType,
                                  @Param("proId") Integer proId,
                                  @Param("eliminated") Integer eliminated);

    /**
     * 新增：如果子表没有该 proId 的记录，插入一条最小记录（仅 pro_id + eliminated）
     */
    int insertMinimalIfNotExists(@Param("proSubType") String proSubType,
                                 @Param("proId") Integer proId,
                                 @Param("eliminated") Integer eliminated);

    /**
     * 已确认淘汰列表（4 张子表 eliminated=1 的项目）
     */
    List<Map<String, Object>> listConfirmedEliminated(@Param("taskId") String taskId);

    /**
     * 统计该专家在该任务下被分派的项目总数（用于确认提交前检查是否全部已评级/已回避）
     */
    int countAssignedProjects(@Param("taskId") String taskId, @Param("uid") Long uid);

    /**
     * 查询项目快照信息（用于评级保存时补全冗余字段）
     * @return { proCode, topicName, groupName, companyName } 或 null
     */
    Map<String, Object> getProjectSnapshotInfo(@Param("proId") Integer proId);

    /**
     * 按 proCode 反查项目信息（导入淘汰名单时用）
     * @return { proId, proCode, proSubType } 或 null
     */
    Map<String, Object> findProInfoByProCode(@Param("taskId") String taskId,
                                             @Param("proCode") String proCode);

    /**
     * 专家评审汇总：全部项目 LEFT JOIN 专家评级，一行一个（项目+专家）组合，用于导出 Excel
     * @return rows: { proSubType, proCode, topicName, groupName, companyName, expertName, grade, remark }
     */
    List<Map<String, Object>> listExpertEvalDetail(@Param("taskId") String taskId,
                                                  @Param("contactUserId") Long contactUserId);

    /**
     * 判断项目是否在指定小组联络人的 surver_view_scope 绑定专家组范围内
     */
    int countProInSurverContactScope(@Param("proId") Integer proId,
                                     @Param("contactUserId") Long contactUserId);

    /**
     * 勘察奖小组联络人：当前任务下 surver_view_scope 可见的项目 id（与 listProInfo 同源）
     */
    List<Integer> listProIdsVisibleToSurverContact(@Param("taskId") String taskId,
                                                  @Param("contactUserId") Long contactUserId);

    /**
     * 专家侧：仅本人淘汰评级及评语（用于「下载淘汰评语」）；项目须在本人专业组分派范围内。
     * 列含：类别、项目编号、项目名称、申报单位、申报账号、分组(QC)、专家分组、专家、评级、淘汰状态、评级理由等
     */
    List<Map<String, Object>> listMyExpertGroupEliminateDetail(@Param("taskId") String taskId,
                                                                @Param("proSubType") String proSubType,
                                                                @Param("viewerUid") Long viewerUid);
}
