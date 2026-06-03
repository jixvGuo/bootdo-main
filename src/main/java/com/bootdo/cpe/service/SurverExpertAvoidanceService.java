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
     * 遍历任务下所有勘察奖项目，按完成单位与专家单位比对，命中则自动创建回避记录
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
