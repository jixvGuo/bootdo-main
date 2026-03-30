package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.QcExpertAvoidanceDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * QC专家回避服务
 * 
 * @author system
 * @date 2026-03-27
 */
public interface QcExpertAvoidanceService {

    /**
     * 查询回避记录
     */
    QcExpertAvoidanceDO get(Integer id);

    /**
     * 查询回避记录列表
     */
    List<QcExpertAvoidanceDO> list(Map<String, Object> map);

    /**
     * 统计回避记录数量
     */
    int count(Map<String, Object> map);

    /**
     * 保存回避记录
     */
    int save(QcExpertAvoidanceDO avoidance);

    /**
     * 删除回避记录
     */
    int remove(Integer id);

    /**
     * 批量删除回避记录
     */
    int batchRemove(Integer[] ids);

    /**
     * 检查专家是否回避某项目
     */
    boolean checkAvoidance(String taskId, Integer proId, Integer expertUserId);

    /**
     * 获取专家在某任务下的所有回避项目ID列表
     */
    List<Integer> getAvoidedProIds(String taskId, Integer expertUserId);

    /**
     * 获取某项目的所有回避专家ID列表（用于平均分计算排除）
     */
    List<Integer> getAvoidedExpertIds(String taskId, Integer proId);

    /**
     * 自动回避：专家创建时检查单位并自动插入回避记录
     * @param taskId 任务ID
     * @param expertUserId 专家用户ID
     * @param expertCompany 专家单位
     * @return 自动回避的项目数量
     */
    int autoAvoidByCompany(String taskId, Integer expertUserId, String expertCompany);

    /**
     * 手动回避：为专家标记某项目为回避
     * @param taskId 任务ID
     * @param proId 项目ID
     * @param expertUserId 专家用户ID
     * @param createdBy 操作人ID
     * @param reason 回避原因
     * @return 是否成功
     */
    boolean manualAvoid(String taskId, Integer proId, Integer expertUserId, Integer createdBy, String reason);

    /**
     * 取消回避
     * @param taskId 任务ID
     * @param proId 项目ID
     * @param expertUserId 专家用户ID
     * @return 是否成功
     */
    boolean cancelAvoidance(String taskId, Integer proId, Integer expertUserId);
}
