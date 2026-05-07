package com.bootdo.cpe.service;

import com.bootdo.cpe.domain.SurverExpertAvoidanceDO;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家回避服务（接口签名 1:1 镜像 QcExpertAvoidanceService）
 */
public interface SurverExpertAvoidanceService {

    SurverExpertAvoidanceDO get(Integer id);

    List<SurverExpertAvoidanceDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertAvoidanceDO avoidance);

    int remove(Integer id);

    int batchRemove(Integer[] ids);

    boolean checkAvoidance(String taskId, Integer proId, Integer expertUserId);

    List<Integer> getAvoidedProIds(String taskId, Integer expertUserId);

    List<Integer> getAvoidedExpertIds(String taskId, Integer proId);

    /**
     * 自动回避（按专家单位 vs 申报单位）
     * 注意: 勘察奖的项目分散在 4 张子表里，自动回避所需的"项目-单位"映射在 Phase C
     * 专家打分页面落地时再补充实现。当前返回 0，stub。
     */
    int autoAvoidByCompany(String taskId, Integer expertUserId, String expertCompany);

    /**
     * 手动回避：检查重复 + 写入
     */
    boolean manualAvoid(String taskId, Integer proId, Integer expertUserId, Integer createdBy, String reason);

    /**
     * 取消回避：把该 (task,pro,expert) 的所有 deleted=0 记录置 deleted=1
     */
    boolean cancelAvoidance(String taskId, Integer proId, Integer expertUserId);
}
