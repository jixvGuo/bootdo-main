package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.QcExpertAvoidanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * QC专家回避记录DAO
 * 
 * @author system
 * @date 2026-03-27
 */
@Mapper
public interface QcExpertAvoidanceDao {

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
     * 批量保存回避记录（自动回避）
     */
    int batchSave(List<QcExpertAvoidanceDO> list);

    /**
     * 删除回避记录（逻辑删除）
     */
    int remove(Integer id);

    /**
     * 批量删除回避记录
     */
    int batchRemove(Integer[] ids);

    /**
     * 检查专家是否回避某项目
     */
    int checkAvoidance(@Param("taskId") String taskId, 
                      @Param("proId") Integer proId, 
                      @Param("expertUserId") Integer expertUserId);

    /**
     * 获取专家在某任务下的所有回避项目ID列表
     */
    List<Integer> getAvoidedProIds(@Param("taskId") String taskId, 
                                   @Param("expertUserId") Integer expertUserId);

    /**
     * 获取某项目的所有回避专家ID列表（用于平均分计算排除）
     */
    List<Integer> getAvoidedExpertIds(@Param("taskId") String taskId, 
                                      @Param("proId") Integer proId);
}
