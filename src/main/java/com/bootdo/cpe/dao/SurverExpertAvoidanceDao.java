package com.bootdo.cpe.dao;

import com.bootdo.cpe.domain.SurverExpertAvoidanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 勘察奖-专家回避记录 DAO（1:1 镜像 QcExpertAvoidanceDao）
 */
@Mapper
public interface SurverExpertAvoidanceDao {

    SurverExpertAvoidanceDO get(Integer id);

    List<SurverExpertAvoidanceDO> list(Map<String, Object> map);

    int count(Map<String, Object> map);

    int save(SurverExpertAvoidanceDO avoidance);

    int batchSave(List<SurverExpertAvoidanceDO> list);

    int remove(Integer id);

    int batchRemove(Integer[] ids);

    int checkAvoidance(@Param("taskId") String taskId,
                       @Param("proId") Integer proId,
                       @Param("expertUserId") Integer expertUserId);

    List<Integer> getAvoidedProIds(@Param("taskId") String taskId,
                                   @Param("expertUserId") Integer expertUserId);

    List<Integer> getAvoidedExpertIds(@Param("taskId") String taskId,
                                      @Param("proId") Integer proId);
}
